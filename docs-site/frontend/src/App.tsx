import { BrowserRouter, Routes, Route, useLocation } from 'react-router-dom';
import { useEffect } from 'react';
import { Home } from './pages/Home';
import { GettingStarted } from './pages/GettingStarted';
import { Features } from './pages/Features';
import { ApiOverview, OrdersApi } from './pages/Api';

// Scroll to top on route change
function ScrollToTop() {
  const { pathname } = useLocation();
  
  useEffect(() => {
    // Scroll window to top
    window.scrollTo(0, 0);
    
    // Reset all overflow-y-auto containers
    document.querySelectorAll('.overflow-y-auto, [data-scroll-reset]').forEach(el => {
      (el as HTMLElement).scrollTop = 0;
    });
    
    // Also scroll main and aside elements
    document.querySelectorAll('main, aside, nav').forEach(el => {
      (el as HTMLElement).scrollTop = 0;
    });
  }, [pathname]);
  
  return null;
}

function App() {
  return (
    <BrowserRouter>
      <ScrollToTop />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/getting-started" element={<GettingStarted />} />
        <Route path="/getting-started/:section" element={<GettingStarted />} />
        <Route path="/features" element={<Features />} />
        <Route path="/features/:feature" element={<Features />} />
        <Route path="/api" element={<ApiOverview />} />
        <Route path="/api/orders" element={<OrdersApi />} />
        {/* Placeholder routes - will be implemented similarly to OrdersApi */}
        <Route path="/api/line-items" element={<ApiOverview />} />
        <Route path="/api/customers" element={<ApiOverview />} />
        <Route path="/api/payments" element={<ApiOverview />} />
        <Route path="/api/webhooks" element={<ApiOverview />} />
        <Route path="/guides" element={<GettingStarted />} />
        <Route path="/guides/:guide" element={<GettingStarted />} />
        <Route path="/changelog" element={<GettingStarted />} />
        <Route path="/faq" element={<GettingStarted />} />
        <Route path="/support" element={<GettingStarted />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
