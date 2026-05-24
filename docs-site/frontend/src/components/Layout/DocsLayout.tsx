import type { ReactNode } from 'react';
import { ApiLayout } from './ApiLayout';

interface DocsLayoutProps {
  children: ReactNode;
}

export function DocsLayout({ children }: DocsLayoutProps) {
  // Use ApiLayout for consistent styling across all pages
  return <ApiLayout>{children}</ApiLayout>;
}
