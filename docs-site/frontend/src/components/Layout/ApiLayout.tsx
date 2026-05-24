import type { ReactNode } from 'react';
import { Header } from './Header';
import { Sidebar } from './Sidebar';
import { apiNavigation } from '../../data/navigation';

interface ApiLayoutProps {
  children: ReactNode;
  rightPanel?: ReactNode;
}

export function ApiLayout({ children, rightPanel }: ApiLayoutProps) {
  return (
    <div className="min-h-screen flex flex-col bg-background">
      <Header />
      
      {/* Main content wrapper - fills remaining height after header */}
      <div className="flex-1 flex pt-16">
        <div className="flex-1 flex max-w-[1800px] w-full mx-auto">
          {/* Left Sidebar - Navigation */}
          <Sidebar 
            navigation={apiNavigation} 
            className="hidden lg:flex flex-col w-64 flex-shrink-0 py-6 pl-6 border-r border-white/5"
          />
          
          {/* Main Content - Documentation */}
          <main className="flex-1 min-w-0 overflow-y-auto py-8 px-6 lg:px-10">
            {children}
          </main>
          
          {/* Right Panel - Code/Sandbox */}
          {rightPanel && (
            <aside className="hidden xl:flex flex-col w-[480px] flex-shrink-0 py-8 pr-6 border-l border-white/5 bg-background-elevated/30">
              <div className="sticky top-20 overflow-y-auto max-h-[calc(100vh-6rem)]">
                {rightPanel}
              </div>
            </aside>
          )}
        </div>
      </div>
    </div>
  );
}
