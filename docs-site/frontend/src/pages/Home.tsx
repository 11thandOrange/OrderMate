import { Header, Footer, Container, Section, Grid } from '../components/Layout';
import { HeroSection } from '../components/composite/HeroSection';
import { FeatureCard } from '../components/composite/FeatureCard';
import { SectionHeader } from '../components/composite/SectionHeader';
import { ApiPreview } from '../components/composite/ApiPreview';
import { DocsCTASection } from '../components/composite/CTASection';
import { Badge } from '../components/ui/Badge';
import { ButtonLink } from '../components/ui/Button';
import { 
  BookOpen, 
  Code2, 
  Zap, 
  ArrowRight, 
  ShoppingBag,
  Calendar,
  Bell,
} from 'lucide-react';

export function Home() {
  return (
    <div className="min-h-screen flex flex-col bg-background">
      <Header />
      
      {/* Hero Section */}
      <HeroSection
        badge={{ text: 'Clover POS Integration', icon: Zap }}
        title={<>OrderMate <span className="text-brand">Documentation</span></>}
        subtitle="Everything you need to integrate OrderMate with your Clover POS system. Learn about features, explore the API, and build powerful integrations."
        ctas={[
          { label: 'Get Started', to: '/getting-started' },
          { label: 'API Reference', to: '/api', variant: 'secondary', icon: Code2 },
        ]}
      />

      {/* Features Grid */}
      <Section spacing="lg">
        <Container>
          <SectionHeader
            title="Explore the Documentation"
            description="Comprehensive guides and references to help you get the most out of OrderMate"
          />
          
          <Grid cols={4} gap="md">
            <FeatureCard
              title="Getting Started"
              description="Learn how to install and configure OrderMate on your Clover device."
              icon={BookOpen}
              to="/getting-started"
            />
            <FeatureCard
              title="Order Management"
              description="Manage orders, track status, and handle customer requests efficiently."
              icon={ShoppingBag}
              to="/features/orders"
            />
            <FeatureCard
              title="Calendar View"
              description="View scheduled orders in day, week, or month format with smart filters."
              icon={Calendar}
              to="/features/calendar"
            />
            <FeatureCard
              title="Notifications"
              description="Send SMS and email notifications to keep customers informed."
              icon={Bell}
              to="/features/notifications"
            />
          </Grid>
        </Container>
      </Section>

      {/* API Section */}
      <Section spacing="lg" background="gradient">
        <Container>
          <div className="grid lg:grid-cols-2 gap-12 items-start">
            {/* Left column - Text */}
            <div className="lg:sticky lg:top-24">
              <Badge variant="success" size="md" className="mb-6">
                <span className="w-2 h-2 bg-status-success rounded-full animate-pulse mr-2" />
                Live Examples
              </Badge>
              
              <h2 className="text-heading-lg text-content mb-4">
                Powerful API Reference
              </h2>
              <p className="text-content-secondary mb-8 leading-relaxed">
                Interactive documentation with live examples. Test API endpoints 
                directly in your browser with our sandbox environment.
              </p>
              
              <ul className="space-y-4 mb-10">
                {[
                  'Orders, Line Items, Customers, Payments',
                  'Webhooks for real-time notifications',
                  'Code examples in cURL, Python, Kotlin',
                ].map((item) => (
                  <li key={item} className="flex items-start gap-3 text-content-secondary">
                    <span className="w-5 h-5 rounded-full bg-status-success/20 flex items-center justify-center mt-0.5 shrink-0">
                      <span className="w-2 h-2 bg-status-success rounded-full" />
                    </span>
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
              
              <ButtonLink to="/api" variant="secondary" rightIcon={<ArrowRight className="w-4 h-4" />}>
                Explore API Reference
              </ButtonLink>
            </div>
            
            {/* Right column - API Preview */}
            <ApiPreview />
          </div>
        </Container>
      </Section>

      {/* CTA Section */}
      <DocsCTASection />

      {/* Footer */}
      <Footer />
    </div>
  );
}
