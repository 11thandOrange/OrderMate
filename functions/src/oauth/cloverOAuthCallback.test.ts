/**
 * Tests for the Clover OAuth install callback (#142 follow-up).
 *
 * Covers: successful code exchange storing a per-merchant token, rejecting
 * a request missing merchant_id/code, and surfacing an exchange failure as
 * 500 instead of silently leaving the merchant with no token.
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
    once: jest.fn(async () => ({val: () => (mockStore[path] as unknown) ?? null})),
  });

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

jest.mock("axios");

// eslint-disable-next-line @typescript-eslint/no-var-requires
import * as admin from "firebase-admin";
import axios from "axios";
import {cloverOAuthCallback} from "./cloverOAuthCallback";

type MockDatabaseFn = {
  __store: Record<string, unknown>;
};

function getStore(): Record<string, unknown> {
  return (admin.database as unknown as MockDatabaseFn).__store;
}

function makeReq(query: Record<string, string>) {
  return {query} as never;
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
  await (cloverOAuthCallback as unknown as (req: unknown, res: unknown) => Promise<void>)(
    req, res
  );
}

const mockedAxios = axios as jest.Mocked<typeof axios>;

describe("cloverOAuthCallback", () => {
  const originalEnv = process.env;

  beforeEach(() => {
    for (const key of Object.keys(getStore())) delete getStore()[key];
    process.env = {
      ...originalEnv,
      CLOVER_CLIENT_ID: "test-client-id",
      CLOVER_CLIENT_SECRET: "test-client-secret",
    };
    mockedAxios.post.mockReset();
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  it("rejects a request missing merchant_id", async () => {
    const res = makeRes();
    await invoke(makeReq({code: "CODE1"}), res);
    expect(res.status).toHaveBeenCalledWith(400);
  });

  it("rejects a request missing code", async () => {
    const res = makeRes();
    await invoke(makeReq({merchant_id: "M1"}), res);
    expect(res.status).toHaveBeenCalledWith(400);
  });

  it("exchanges the code and stores a merchant-specific token, not a shared one", async () => {
    mockedAxios.post.mockResolvedValueOnce({
      data: {access_token: "AT1", refresh_token: "RT1", expires_in: 1800},
    });
    const res = makeRes();

    await invoke(makeReq({merchant_id: "M1", code: "CODE1"}), res);

    expect(res.status).toHaveBeenCalledWith(200);
    expect(mockedAxios.post).toHaveBeenCalledWith(
      expect.stringContaining("/oauth/v2/token"),
      expect.objectContaining({
        client_id: "test-client-id",
        client_secret: "test-client-secret",
        code: "CODE1",
      }),
      expect.anything()
    );
    expect(getStore()["merchants/M1/cloverAuth"]).toMatchObject({
      accessToken: "AT1",
      refreshToken: "RT1",
      merchantId: "M1",
    });
  });

  it("keeps two merchants' tokens independent", async () => {
    mockedAxios.post
      .mockResolvedValueOnce({
        data: {access_token: "AT_M1", refresh_token: "RT_M1", expires_in: 1800},
      })
      .mockResolvedValueOnce({
        data: {access_token: "AT_M2", refresh_token: "RT_M2", expires_in: 1800},
      });

    await invoke(makeReq({merchant_id: "M1", code: "CODE1"}), makeRes());
    await invoke(makeReq({merchant_id: "M2", code: "CODE2"}), makeRes());

    expect(getStore()["merchants/M1/cloverAuth"]).toMatchObject({accessToken: "AT_M1"});
    expect(getStore()["merchants/M2/cloverAuth"]).toMatchObject({accessToken: "AT_M2"});
  });

  it("returns 500 and stores nothing when the exchange fails", async () => {
    mockedAxios.post.mockRejectedValueOnce(new Error("Clover rejected the code"));
    const res = makeRes();

    await invoke(makeReq({merchant_id: "M1", code: "BAD_CODE"}), res);

    expect(res.status).toHaveBeenCalledWith(500);
    expect(getStore()["merchants/M1/cloverAuth"]).toBeUndefined();
  });
});
