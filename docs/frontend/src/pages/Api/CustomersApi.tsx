import { ApiCategoryPage } from './ApiCategoryPage';

export function CustomersApi() {
  return (
    <ApiCategoryPage
      categorySlug="customers"
      title="Customers"
      description="Manage the customer records attached to orders - contact info, and the order history each customer builds up over time."
    />
  );
}
