import { ApiCategoryPage } from './ApiCategoryPage';

export function LineItemsApi() {
  return (
    <ApiCategoryPage
      categorySlug="line-items"
      title="Line Items"
      description="Line items represent the individual products within an order. Add, update, or remove them as an order is being built or edited."
    />
  );
}
