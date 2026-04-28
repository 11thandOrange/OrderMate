#!/usr/bin/env python3
"""
Migration script to convert legacy Firebase customData to new merchants schema.

Mapping:
- Order 0: CALENDAR       <- PickUpDate
- Order 1: SINGLE_SELECT  <- OrderCategories (Category)
- Order 2: SINGLE_SELECT  <- OrderSubCategories (Sub-Category)
- Order 3: MULTI_SELECT   <- OrderType (Type)
- Order 4: TEXT_BOX       <- Description
- Order 5: SINGLE_SELECT  <- OrderProgress (Status)
"""

import json
import uuid
import time
import sys

WIDGET_MAPPING = [
    {"order": 0, "type": "CALENDAR",      "label": "PickUp Date",   "legacyType": "PickUpDate"},
    {"order": 1, "type": "SINGLE_SELECT", "label": "Category",      "legacyType": "OrderCategories"},
    {"order": 2, "type": "SINGLE_SELECT", "label": "Sub-Category",  "legacyType": "OrderSubCategories"},
    {"order": 3, "type": "MULTI_SELECT",  "label": "Type",          "legacyType": "OrderType"},
    {"order": 4, "type": "TEXT_BOX",      "label": "Description",   "legacyType": "Description"},
    {"order": 5, "type": "SINGLE_SELECT", "label": "Status",        "legacyType": "OrderProgress"},
]

DEFAULT_SETTINGS = {
    "itemNotesEnabled": True,
    "notificationDays": 3,
    "notificationMinutes": 0,
    "orderNotesEnabled": True,
    "printNotesOnCustomerReceipts": False,
    "printNotesOnOrderReceipts": True,
    "receiptDays": 0,
    "receiptMinutes": 60,
    "scheduledNotificationsEnabled": False,
    "scheduledReceiptEnabled": False,
    "showOMButtonInRegister": False,
    "triggerOnItemAdd": False,
    "useOrderMateInRegister": False,
    "useOrderMateRegisterInstead": True
}

def generate_uuid():
    return str(uuid.uuid4())

def create_options_from_list(option_list):
    """Convert legacy list array to new options object with UUIDs."""
    if not option_list:
        return None
    
    options = {}
    for i, value in enumerate(option_list):
        option_id = generate_uuid()
        options[option_id] = {
            "isDefault": i == 0,  # First option is default
            "label": value,
            "value": value
        }
    return options

def create_widget(mapping, legacy_type):
    """Create a new widget from legacy type data."""
    widget_id = generate_uuid()
    
    widget = {
        "isEnabled": legacy_type.get("isActive", False),
        "isRequired": False,
        "label": mapping["label"],
        "level": "ITEM",
        "order": mapping["order"],
        "showInFilter": True,
        "type": mapping["type"]
    }
    
    # Add options for SELECT types if legacy has list items
    if mapping["type"] in ["SINGLE_SELECT", "MULTI_SELECT"]:
        legacy_list = legacy_type.get("list", [])
        if legacy_list:
            widget["options"] = create_options_from_list(legacy_list)
    
    return widget_id, widget

def create_default_template():
    """Create default email template."""
    template_id = generate_uuid()
    template = {
        "content": "Your order from {{merchant_name}} is ready for pickup!",
        "name": "Order Ready",
        "subject": "Hello, {{customer_name}}! Your Order Is Ready 🔥"
    }
    return template_id, template

def migrate_merchant(merchant_id, legacy_data_str):
    """Migrate a single merchant from legacy to new schema."""
    # Parse the legacy data JSON string
    legacy_data = json.loads(legacy_data_str)
    types_array = legacy_data.get("types", [])
    
    # Create lookup for legacy types by type key
    legacy_types_lookup = {t["type"]: t for t in types_array}
    
    # Build widgets
    widgets = {}
    for mapping in WIDGET_MAPPING:
        legacy_type = legacy_types_lookup.get(mapping["legacyType"], {})
        if legacy_type:
            widget_id, widget = create_widget(mapping, legacy_type)
            widgets[widget_id] = widget
    
    # Build template
    template_id, template = create_default_template()
    
    # Build merchant object
    merchant = {
        "meta": {
            "updatedAt": int(time.time() * 1000)
        },
        "settings": DEFAULT_SETTINGS.copy(),
        "templates": {
            template_id: template
        },
        "widgets": widgets
    }
    
    return merchant

def migrate_all(input_data):
    """Migrate all legacy customData to new merchants schema."""
    output = {
        "customData": input_data.get("customData", {}),  # Keep original
        "merchants": input_data.get("merchants", {}).copy()  # Keep existing
    }
    
    custom_data = input_data.get("customData", {})
    
    for merchant_id, merchant_data in custom_data.items():
        # Skip if merchant already exists in merchants
        if merchant_id in output["merchants"]:
            print(f"SKIP: {merchant_id} - already exists in merchants", file=sys.stderr)
            continue
        
        legacy_data_str = merchant_data.get("data", "{}")
        migrated = migrate_merchant(merchant_id, legacy_data_str)
        output["merchants"][merchant_id] = migrated
        print(f"MIGRATED: {merchant_id}", file=sys.stderr)
    
    return output

def main():
    # Read from input.json file
    with open("input.json", "r", encoding="utf-8") as f:
        input_data = json.load(f)
    
    output_data = migrate_all(input_data)
    
    # Write to output.json file - ensure_ascii=False preserves emojis
    with open("output.json", "w", encoding="utf-8") as f:
        json.dump(output_data, f, indent=2, ensure_ascii=False)
    
    # Also print to stdout
    print(json.dumps(output_data, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()
