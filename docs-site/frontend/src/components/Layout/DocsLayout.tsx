import type { ReactNode } from 'react';
import { Header } from './Header';
import { Sidebar } from './Sidebar';
import { Footer } from './Footer';
import { navigation } from '../../data/navigation';

interface DocsLayoutProps {
  children: ReactNode;
}

export function DocsLayout({ children }: DocsLayoutProps) {
  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      <div className="flex-1 flex pt-16">
        <div className="flex-1 flex max-w-[1400px] w-full mx-auto">
          {/* Left Sidebar */}
          <Sidebar
            navigation={navigation}
            className="hidden lg:flex flex-col w-64 flex-shrink-0 py-6 pl-6 border-r"
            style={{ borderColor: 'rgba(255,255,255,0.1)' }}
          />

          {/* Main Content */}
          <main className="flex-1 min-w-0 overflow-y-auto py-8 px-6 lg:px-10">
            <div className="max-w-3xl">
              {children}
            </div>
          </main>
        </div>
      </div>

      <Footer />
    </div>
  );
}
