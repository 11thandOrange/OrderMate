export function DocsCTASection() {
  return (
    <section className="py-[120px] px-10 text-center relative">
      <div className="max-w-[800px] mx-auto">
        <h2 className="text-[48px] font-bold text-content mb-5 tracking-tight">
          Ready to Transform Your Order Management?
        </h2>
        <p className="text-xl text-content-secondary mb-10">
          Join merchants who are saving time and delighting customers with OrderMate.
        </p>
        <div className="flex flex-wrap gap-4 justify-center">
          <a
            href="https://www.clover.com/appmarket/apps/WWTF1AKT87VJ8"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 px-10 py-[18px] rounded-full text-white font-semibold text-[17px] transition-all duration-300 no-underline hover:-translate-y-0.5"
            style={{
              background: '#FF9F43',
              boxShadow: '0 4px 16px rgba(255,159,67,0.4)',
            }}
            onMouseEnter={(e) => {
              (e.currentTarget as HTMLAnchorElement).style.background = '#e68a2e';
              (e.currentTarget as HTMLAnchorElement).style.boxShadow = '0 8px 24px rgba(255,159,67,0.5)';
            }}
            onMouseLeave={(e) => {
              (e.currentTarget as HTMLAnchorElement).style.background = '#FF9F43';
              (e.currentTarget as HTMLAnchorElement).style.boxShadow = '0 4px 16px rgba(255,159,67,0.4)';
            }}
          >
            Install OrderMate on Clover App Market
          </a>
        </div>
      </div>
    </section>
  );
}
