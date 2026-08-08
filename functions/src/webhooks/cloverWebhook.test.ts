/**
 * Tests for the Clover webhook handler (#142).
 *
 * Covers: the verification handshake, both supported payload formats, auth
 * rejection, and idempotent event writes on a simulated Clover retry.
 */

jest.mock("firebase-admin", () => {
  const mockStore: Record<string, unknown> = {};

  const mockRef = (path: string) => ({
    set: jest.fn(async (value: unknown) => {
      mockStore[path] = value;
    }),
    update: jest.fn(async (value: Record<string, unknown>) => {
      mockStore[path] = {
        ...((mockStore[path] as Record<string, unknown>) || {}),
        ...value,
      };
    }),
    push: jest.fn(() => ({key: "mock-push-key"})),
    once: jest.fn(async () => ({val: () => (mockStore[path] as unknown) ?? null})),
  });

  // A single shared instance - admin.database() must always return the SAME
  // object, since cloverWebhook.ts captures it once at module load. Returning
  // a fresh object per call would make jest.spyOn() in tests silently target
  // an instance the module under test never actually uses.
  const mockDbInstance = {ref: mockRef};
  const mockDatabaseFn = jest.fn(() => mockDbInstance) as unknown as {
    (): {ref: typeof mockRef};
    ServerValue: {TIMESTAMP: string};
    __store: Record<string, unknown>;
  };
  mockDatabaseFn.ServerValue = {TIMESTAMP: "MOCK_TIMESTAMP"};
  mockDatabaseFn.__store = mockStore;

  return {
    initializeApp: jest.fn(),
    database: mockDatabaseFn,
  };
});

// eslint-disable-next-line @typescript-eslint/no-var-requires
import * as admin from "firebase-admin";
import {cloverWebhook} from "./cloverWebhook";

type MockDatabaseFn = {
  __store: Record<string, unknown>;
};

function getStore(): Record<string, unknown> {
  return (admin.database as unknown as MockDatabaseFn).__store;
}

function makeReq(
  body: Record<string, unknown>,
  headers: Record<string, string> = {},
  method = "POST"
) {
  return {method, body, headers} as never;
}

function makeRes() {
  const res: {status: jest.Mock; send: jest.Mock} = {
    status: jest.fn(),
    send: jest.fn(),
  };
  res.status.mockReturnValue(res);
  return res as never as {status: jest.Mock; send: jest.Mock};
}

async function invoke(req: unknown, res: {status: jest.Mock; send: jest.Mock}) {
  // functions.https.onRequest's export is directly callable as (req, res).
  await (cloverWebhook as unknown as (req: unknown, res: unknown) => Promise<void>)(
    req, res
  );
}

const AUTH_HEADER = {"x-clover-auth": "test-secret"};

describe("cloverWebhook", () => {
  const originalEnv = process.env;

  beforeEach(() => {
    for (const key of Object.keys(getStore())) delete getStore()[key];
    process.env = {...originalEnv, CLOVER_AUTH_CODE: "test-secret"};
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  it("responds OK to a GET health check with no auth required", async () => {
    const res = makeRes();
    await invoke(makeReq({}, {}, "GET"), res);

    expect(res.status).toHaveBeenCalledWith(200);
  });

  it("rejects methods other than GET/POST", async () => {
    const res = makeRes();
    await invoke(makeReq({}, {}, "PUT"), res);

    expect(res.status).toHaveBeenCalledWith(405);
  });

  it("handles the verification handshake without requiring auth", async () => {
    delete process.env.CLOVER_AUTH_CODE;
    const res = makeRes();

    await invoke(makeReq({verificationCode: "abc123"}), res);

    expect(res.status).toHaveBeenCalledWith(200);
  });

  it("rejects a real event when CLOVER_AUTH_CODE is not configured", async () => {
    delete process.env.CLOVER_AUTH_CODE;
    const res = makeRes();

    await invoke(
      makeReq({merchantId: "M1", type: "APP_INSTALLED"}, AUTH_HEADER),
      res
    );

    expect(res.status).toHaveBeenCalledWith(401);
  });

  it("rejects a real event with a missing or wrong x-clover-auth header", async () => {
    const res = makeRes();

    await invoke(
      makeReq({merchantId: "M1", type: "APP_INSTALLED"}, {"x-clover-auth": "wrong"}),
      res
    );

    expect(res.status).toHaveBeenCalledWith(401);
  });

  it("processes a legacy-format APP_INSTALLED event with valid auth", async () => {
    const res = makeRes();

    await invoke(
      makeReq({merchantId: "M1", type: "APP_INSTALLED"}, AUTH_HEADER),
      res
    );

    expect(res.status).toHaveBeenCalledWith(200);
    expect(getStore()["merchants/M1/merchantInfo"]).toBeDefined();
    expect(getStore()["merchants/M1/subscription"]).toMatchObject({plan: "free"});
  });

  it("rejects an unrecognized payload shape", async () => {
    const res = makeRes();

    await invoke(makeReq({foo: "bar"}, AUTH_HEADER), res);

    expect(res.status).toHaveBeenCalledWith(400);
  });

  it("processes a Clover-standard-format install event and writes a deterministic event key", async () => {
    const res = makeRes();

    await invoke(
      makeReq({
        appId: "APP1",
        merchants: {
          M1: [{objectId: "A:APP1", type: "CREATE", ts: 1700000000000}],
        },
      }, AUTH_HEADER),
      res
    );

    expect(res.status).toHaveBeenCalledWith(200);
    expect(getStore()["merchants/M1/events/M1_INSTALL_1700000000000"]).toBeDefined();
  });

  it("does not create a duplicate event record when Clover retries the same delivery", async () => {
    const res1 = makeRes();
    const res2 = makeRes();
    const payload = makeReq({
      appId: "APP1",
      merchants: {
        M1: [{objectId: "A:APP1", type: "CREATE", ts: 1700000000000}],
      },
    }, AUTH_HEADER);

    await invoke(payload, res1);
    await invoke(payload, res2);

    const eventKeys = Object.keys(getStore()).filter((k) =>
      k.startsWith("merchants/M1/events/"));
    expect(eventKeys).toHaveLength(1);
  });

  it("still processes remaining merchants when one fails, but reports failure so Clover retries", async () => {
    const res = makeRes();
    const mockDb = admin.database as unknown as () => {ref: (path: string) => unknown};
    const realRef = mockDb().ref;
    const refSpy = jest.spyOn(mockDb(), "ref").mockImplementation((path: string) => {
      if (path === "merchants/BAD/merchantInfo") {
        return {
          set: jest.fn(async () => {
            throw new Error("simulated failure");
          }),
          update: jest.fn(),
          push: jest.fn(() => ({key: "mock-push-key"})),
        };
      }
      return realRef(path);
    });

    await invoke(
      makeReq({
        appId: "APP1",
        merchants: {
          BAD: [{objectId: "A:APP1", type: "CREATE", ts: 1}],
          GOOD: [{objectId: "A:APP1", type: "CREATE", ts: 2}],
        },
      }, AUTH_HEADER),
      res
    );

    // BAD's failure must not be reported as success - Clover needs a non-2xx
    // to know to retry this delivery, or the failed write is lost silently.
    expect(res.status).toHaveBeenCalledWith(500);
    // GOOD must still have been processed despite BAD's failure.
    expect(getStore()["merchants/GOOD/merchantInfo"]).toBeDefined();
    // BAD's failure must not have silently written partial/incorrect data.
    expect(getStore()["merchants/BAD/merchantInfo"]).toBeUndefined();

    refSpy.mockRestore();
  });
});
