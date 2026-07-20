import { ApiCategoryPage } from './ApiCategoryPage';

export function PaymentsApi() {
  return (
    <ApiCategoryPage
      categorySlug="payments"
      title="Payments"
      description="View payment transactions for an order, and issue refunds or voids. Payments are created by the Clover device at checkout, not through this API."
    />
  );
}
