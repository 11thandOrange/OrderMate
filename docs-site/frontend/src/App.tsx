import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Home } from './pages/Home';
import { GettingStarted } from './pages/GettingStarted';
import { Features } from './pages/Features';
import { ApiOverview, OrdersApi } from './pages/Api';

function App() {
  return (
    <BrowserRouter>
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
