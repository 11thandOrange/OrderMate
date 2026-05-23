import type { ReactNode } from 'react';
import { Header } from './Header';
import { Sidebar } from './Sidebar';
import { navigation } from '../../data/navigation';

interface DocsLayoutProps {
  children: ReactNode;
}

export function DocsLayout({ children }: DocsLayoutProps) {
  return (
    <div className="min-h-screen bg-[#0f0f10]">
      <Header />
      
      <div className="flex max-w-[1400px] mx-auto pt-16">
        {/* Left Sidebar - Navigation */}
        <Sidebar 
          navigation={navigation} 
          className="hidden lg:block py-8 pl-6 border-r border-white/5"
        />
        
        {/* Main Content */}
        <main className="flex-1 min-w-0 py-8 px-6 lg:px-10 max-w-3xl">
          {children}
        </main>
      </div>
    </div>
  );
}
