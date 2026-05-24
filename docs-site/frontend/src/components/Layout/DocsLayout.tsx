import type { ReactNode } from 'react';
import { Header } from './Header';
import { Sidebar } from './Sidebar';
import { navigation } from '../../data/navigation';

interface DocsLayoutProps {
  children: ReactNode;
}

export function DocsLayout({ children }: DocsLayoutProps) {
  return (
    <div className="min-h-screen flex flex-col bg-background">
      <Header />

      <div className="flex-1 flex pt-16">
        <div className="flex-1 flex max-w-[1800px] w-full mx-auto">
          <Sidebar
            navigation={navigation}
            className="hidden lg:flex flex-col w-64 flex-shrink-0 py-6 pl-6 border-r border-white/5"
          />

          <main className="flex-1 min-w-0 overflow-y-auto py-8 px-6 lg:px-10">
            <div className="max-w-3xl">
              {children}
            </div>
          </main>
        </div>
      </div>
    </div>
  );
}
