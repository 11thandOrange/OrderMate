from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional
import random
import time

router = APIRouter()


class MockRequest(BaseModel):
    method: str
    path: str
    body: Optional[dict] = None


class MockResponse(BaseModel):
    status: int
    status_text: str
    headers: dict
    data: dict
    time_ms: int


# Mock data generators
def generate_order_id():
    return "".join(random.choices("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", k=13))


def generate_orders(count: int = 5):
    statuses = ["OPEN", "PAID", "PARTIALLY_PAID", "REFUNDED"]
    orders = []
    for i in range(count):
        orders.append({
            "id": generate_order_id(),
            "currency": "USD",
            "total": random.randint(500, 10000),
            "paymentState": random.choice(statuses),
            "title": f"Order #{1000 + i}",
            "note": random.choice(["", "Extra napkins", "No onions", "Rush order"]),
            "createdTime": int(time.time() * 1000) - random.randint(0, 86400000),
            "modifiedTime": int(time.time() * 1000),
            "state": "open",
        })
    return orders


def generate_customers(count: int = 5):
    first_names = ["John", "Jane", "Mike", "Sarah", "David", "Emma"]
    last_names = ["Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia"]
    customers = []
    for i in range(count):
        first = random.choice(first_names)
        last = random.choice(last_names)
        customers.append({
            "id": generate_order_id(),
            "firstName": first,
            "lastName": last,
            "emailAddresses": [{"emailAddress": f"{first.lower()}.{last.lower()}@example.com"}],
            "phoneNumbers": [{"phoneNumber": f"555-{random.randint(1000, 9999)}"}],
        })
    return customers


def generate_line_items(count: int = 3):
    items = ["Burger", "Fries", "Soda", "Pizza", "Salad", "Sandwich", "Coffee", "Tea"]
    line_items = []
    for i in range(count):
        line_items.append({
            "id": generate_order_id(),
            "name": random.choice(items),
            "price": random.randint(199, 1999),
            "quantity": random.randint(1, 3),
        })
    return line_items


def generate_payments(count: int = 2):
    card_types = ["VISA", "MASTERCARD", "AMEX", "DISCOVER"]
    payments = []
    for i in range(count):
        payments.append({
            "id": generate_order_id(),
            "amount": random.randint(500, 5000),
            "result": "SUCCESS",
            "cardTransaction": {
                "last4": str(random.randint(1000, 9999)),
                "cardType": random.choice(card_types),
            },
            "createdTime": int(time.time() * 1000) - random.randint(0, 3600000),
        })
    return payments


@router.post("/request", response_model=MockResponse)
async def mock_request(request: MockRequest):
    """
    Return mock data for API requests.
    Useful for testing without a real Clover account.
    """
    # Simulate network latency
    latency = random.randint(50, 200)
    
    path = request.path.lower()
    
    # Route to appropriate mock data
    if "/orders" in path and request.method == "GET":
        if "{orderid}" in path or path.endswith("/orders/"):
            # Single order
            data = generate_orders(1)[0]
            data["lineItems"] = {"elements": generate_line_items()}
        else:
            # List orders
            data = {"elements": generate_orders(), "href": request.path}
    
    elif "/customers" in path and request.method == "GET":
        if "{customerid}" in path:
            data = generate_customers(1)[0]
        else:
            data = {"elements": generate_customers(), "href": request.path}
    
    elif "/line_items" in path and request.method == "GET":
        data = {"elements": generate_line_items(), "href": request.path}
    
    elif "/payments" in path and request.method == "GET":
        data = {"elements": generate_payments(), "href": request.path}
    
    elif "/webhooks" in path:
        if request.method == "GET":
            data = {
                "elements": [
                    {
                        "id": generate_order_id(),
                        "url": "https://myapp.com/webhooks/clover",
                        "events": ["ORDER_CREATED", "ORDER_UPDATED"],
                        "active": True,
                    }
                ]
            }
        elif request.method == "POST":
            data = {
                "id": generate_order_id(),
                "url": request.body.get("url", "https://example.com/webhook") if request.body else "https://example.com/webhook",
                "events": request.body.get("events", ["ORDER_CREATED"]) if request.body else ["ORDER_CREATED"],
                "active": True,
                "secret": f"whsec_{''.join(random.choices('abcdef0123456789', k=24))}",
            }
    
    elif request.method == "POST":
        # Generic POST response (create)
        data = {"id": generate_order_id(), "href": request.path}
    
    elif request.method == "DELETE":
        data = {}
        return MockResponse(
            status=204,
            status_text="No Content",
            headers={"content-type": "application/json"},
            data=data,
            time_ms=latency,
        )
    
    else:
        data = {"message": "Mock endpoint", "path": request.path}
    
    return MockResponse(
        status=200,
        status_text="OK",
        headers={"content-type": "application/json"},
        data=data,
        time_ms=latency,
    )
