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
    <div className="min-h-screen bg-[#0f0f10]">
      <Header />
      
      <div className="flex max-w-[1800px] mx-auto pt-16">
        {/* Left Sidebar - Navigation */}
        <Sidebar 
          navigation={apiNavigation} 
          className="hidden lg:block py-8 pl-6 border-r border-white/5"
        />
        
        {/* Main Content - Documentation */}
        <main className="flex-1 min-w-0 py-8 px-6 lg:px-10">
          {children}
        </main>
        
        {/* Right Panel - Code/Sandbox */}
        {rightPanel && (
          <aside className="hidden xl:block w-[480px] shrink-0 py-8 pr-6 border-l border-white/5">
            <div className="sticky top-20">
              {rightPanel}
            </div>
          </aside>
        )}
      </div>
    </div>
  );
}
