/**
 * Weekly Report Cron Job
 * Issue #94: Create weekly cron jobs for webhook events
 * 
 * Runs every Monday at 9:00 AM EST
 * Sends email report with:
 * - All installs
 * - All uninstalls
 * - Refunds
 * - Discounts/promo codes used
 */

import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { sendPlainEmail } from '../email/mailchimp';

const db = admin.database();

interface MerchantSummary {
    merchantId: string;
    name: string;
    email: string;
    storeName: string;
    date: Date;
}

interface RefundSummary {
    merchantId: string;
    name: string;
    amount: number;
    date: Date;
}

interface DiscountSummary {
    merchantId: string;
    name: string;
    discountCode: string;
    amount: number;
    date: Date;
}

interface WeeklyReport {
    period: { start: Date; end: Date };
    installs: MerchantSummary[];
    uninstalls: MerchantSummary[];
    refunds: RefundSummary[];
    discountsUsed: DiscountSummary[];
    summary: {
        totalInstalls: number;
        totalUninstalls: number;
        netGrowth: number;
        totalRefunds: number;
        totalDiscountValue: number;
    };
}

/**
 * Weekly report scheduled function
 * Runs every Monday at 9:00 AM EST (14:00 UTC)
 */
export const weeklyReport = functions.pubsub
    .schedule('0 14 * * 1') // 9:00 AM EST = 14:00 UTC
    .timeZone('America/New_York')
    .onRun(async (context) => {
        console.log('Starting weekly report generation');

        try {
            const report = await generateWeeklyReport();
            await sendReportEmail(report);
            console.log('Weekly report sent successfully');
        } catch (error) {
            console.error('Error generating weekly report:', error);
        }

        return null;
    });

/**
 * Generate weekly report data by querying all merchants
 */
async function generateWeeklyReport(): Promise<WeeklyReport> {
    const now = new Date();
    const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
    const weekAgoTimestamp = weekAgo.getTime();

    // Query all merchants
    const merchantsSnapshot = await db.ref('merchants').once('value');
    const merchants = merchantsSnapshot.val() || {};

    const installs: MerchantSummary[] = [];
    const uninstalls: MerchantSummary[] = [];
    const refunds: RefundSummary[] = [];
    const discountsUsed: DiscountSummary[] = [];

    for (const [merchantId, data] of Object.entries(merchants)) {
        const merchantData = data as Record<string, unknown>;
        const info = (merchantData.merchantInfo || {}) as Record<string, unknown>;
        const events = (merchantData.events || {}) as Record<string, Record<string, unknown>>;

        // Process events from the past week
        for (const [, event] of Object.entries(events)) {
            const timestamp = event.timestamp as number;
            if (!timestamp || timestamp < weekAgoTimestamp) continue;

            const eventDate = new Date(timestamp);
            const eventType = event.type as string;
            const details = (event.details || {}) as Record<string, unknown>;

            switch (eventType) {
                case 'INSTALL':
                    installs.push({
                        merchantId,
                        name: (info.name as string) || 'Unknown',
                        email: (info.email as string) || 'N/A',
                        storeName: (info.storeName as string) || 'Unknown Store',
                        date: eventDate
                    });
                    break;

                case 'UNINSTALL':
                    uninstalls.push({
                        merchantId,
                        name: (info.name as string) || 'Unknown',
                        email: (info.email as string) || 'N/A',
                        storeName: (info.storeName as string) || 'Unknown Store',
                        date: eventDate
                    });
                    break;

                case 'REFUND':
                    refunds.push({
                        merchantId,
                        name: (info.name as string) || 'Unknown',
                        amount: (details.amount as number) || 0,
                        date: eventDate
                    });
                    break;

                case 'DISCOUNT_APPLIED':
                    discountsUsed.push({
                        merchantId,
                        name: (info.name as string) || 'Unknown',
                        discountCode: (details.discountCode as string) || '',
                        amount: (details.amount as number) || 0,
                        date: eventDate
                    });
                    break;
            }
        }
    }

    return {
        period: { start: weekAgo, end: now },
        installs,
        uninstalls,
        refunds,
        discountsUsed,
        summary: {
            totalInstalls: installs.length,
            totalUninstalls: uninstalls.length,
            netGrowth: installs.length - uninstalls.length,
            totalRefunds: refunds.reduce((sum, r) => sum + r.amount, 0),
            totalDiscountValue: discountsUsed.reduce((sum, d) => sum + d.amount, 0)
        }
    };
}

/**
 * Send the weekly report via email
 */
async function sendReportEmail(report: WeeklyReport): Promise<void> {
    const adminEmail = functions.config().admin?.email;
    if (!adminEmail) {
        console.error('Admin email not configured');
        return;
    }

    const subject = `OrderMate Weekly Report - ${formatDate(report.period.start)} to ${formatDate(report.period.end)}`;
    const html = generateReportHTML(report);

    await sendPlainEmail(adminEmail, subject, html);
}

/**
 * Generate HTML content for the report email
 */
function generateReportHTML(report: WeeklyReport): string {
    return `
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; color: #333; }
        .header { background: linear-gradient(135deg, #3C4B80, #5A6BA8); color: white; padding: 30px; border-radius: 12px 12px 0 0; }
        .header h1 { margin: 0 0 10px 0; font-size: 24px; }
        .header p { margin: 0; opacity: 0.9; }
        .summary { display: flex; gap: 15px; padding: 20px; background: #f8f9fa; flex-wrap: wrap; }
        .stat { background: white; padding: 20px; border-radius: 8px; text-align: center; flex: 1; min-width: 120px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .stat-value { font-size: 36px; font-weight: bold; color: #3C4B80; }
        .stat-value.positive { color: #28a745; }
        .stat-value.negative { color: #dc3545; }
        .stat-label { color: #666; font-size: 14px; margin-top: 5px; }
        .section { margin: 30px 0; }
        .section-title { font-size: 18px; color: #3C4B80; border-bottom: 2px solid #3C4B80; padding-bottom: 10px; margin-bottom: 15px; }
        table { width: 100%; border-collapse: collapse; margin: 15px 0; font-size: 14px; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #e9ecef; }
        th { background: #f8f9fa; font-weight: 600; color: #495057; }
        tr:hover { background: #f8f9fa; }
        code { background: #e9ecef; padding: 2px 6px; border-radius: 4px; font-size: 12px; }
        .empty { color: #6c757d; font-style: italic; padding: 20px; text-align: center; background: #f8f9fa; border-radius: 8px; }
        .footer { text-align: center; color: #6c757d; font-size: 12px; margin-top: 40px; padding-top: 20px; border-top: 1px solid #e9ecef; }
    </style>
</head>
<body>
    <div class="header">
        <h1>📊 OrderMate Weekly Report</h1>
        <p>${formatDate(report.period.start)} - ${formatDate(report.period.end)}</p>
    </div>
    
    <div class="summary">
        <div class="stat">
            <div class="stat-value">${report.summary.totalInstalls}</div>
            <div class="stat-label">New Installs</div>
        </div>
        <div class="stat">
            <div class="stat-value">${report.summary.totalUninstalls}</div>
            <div class="stat-label">Uninstalls</div>
        </div>
        <div class="stat">
            <div class="stat-value ${report.summary.netGrowth >= 0 ? 'positive' : 'negative'}">
                ${report.summary.netGrowth >= 0 ? '+' : ''}${report.summary.netGrowth}
            </div>
            <div class="stat-label">Net Growth</div>
        </div>
        <div class="stat">
            <div class="stat-value">$${report.summary.totalRefunds.toFixed(2)}</div>
            <div class="stat-label">Refunds</div>
        </div>
    </div>
    
    <div class="section">
        <h2 class="section-title">📥 New Installs (${report.installs.length})</h2>
        ${report.installs.length > 0 ? `
        <table>
            <tr>
                <th>Store Name</th>
                <th>Owner</th>
                <th>Email</th>
                <th>Merchant ID</th>
                <th>Install Date</th>
            </tr>
            ${report.installs.map(i => `
            <tr>
                <td>${escapeHtml(i.storeName)}</td>
                <td>${escapeHtml(i.name)}</td>
                <td>${escapeHtml(i.email)}</td>
                <td><code>${i.merchantId}</code></td>
                <td>${formatDate(i.date)}</td>
            </tr>
            `).join('')}
        </table>
        ` : '<div class="empty">No new installs this week.</div>'}
    </div>
    
    <div class="section">
        <h2 class="section-title">📤 Uninstalls (${report.uninstalls.length})</h2>
        ${report.uninstalls.length > 0 ? `
        <table>
            <tr>
                <th>Store Name</th>
                <th>Owner</th>
                <th>Email</th>
                <th>Merchant ID</th>
                <th>Uninstall Date</th>
            </tr>
            ${report.uninstalls.map(u => `
            <tr>
                <td>${escapeHtml(u.storeName)}</td>
                <td>${escapeHtml(u.name)}</td>
                <td>${escapeHtml(u.email)}</td>
                <td><code>${u.merchantId}</code></td>
                <td>${formatDate(u.date)}</td>
            </tr>
            `).join('')}
        </table>
        ` : '<div class="empty">No uninstalls this week.</div>'}
    </div>
    
    <div class="section">
        <h2 class="section-title">💰 Refunds (${report.refunds.length})</h2>
        ${report.refunds.length > 0 ? `
        <table>
            <tr>
                <th>Merchant</th>
                <th>Amount</th>
                <th>Date</th>
            </tr>
            ${report.refunds.map(r => `
            <tr>
                <td>${escapeHtml(r.name)}</td>
                <td>$${r.amount.toFixed(2)}</td>
                <td>${formatDate(r.date)}</td>
            </tr>
            `).join('')}
        </table>
        ` : '<div class="empty">No refunds this week.</div>'}
    </div>
    
    <div class="section">
        <h2 class="section-title">🏷️ Discounts Used (${report.discountsUsed.length})</h2>
        ${report.discountsUsed.length > 0 ? `
        <table>
            <tr>
                <th>Merchant</th>
                <th>Discount Code</th>
                <th>Amount</th>
                <th>Date</th>
            </tr>
            ${report.discountsUsed.map(d => `
            <tr>
                <td>${escapeHtml(d.name)}</td>
                <td><code>${escapeHtml(d.discountCode)}</code></td>
                <td>$${d.amount.toFixed(2)}</td>
                <td>${formatDate(d.date)}</td>
            </tr>
            `).join('')}
        </table>
        ` : '<div class="empty">No discounts used this week.</div>'}
    </div>
    
    <div class="footer">
        <p>This report was automatically generated by OrderMate Analytics.</p>
        <p>Generated on ${new Date().toLocaleString('en-US', { timeZone: 'America/New_York' })} EST</p>
    </div>
</body>
</html>
    `;
}

/**
 * Format date for display
 */
function formatDate(date: Date): string {
    return date.toLocaleDateString('en-US', {
        weekday: 'short',
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

/**
 * Escape HTML special characters
 */
function escapeHtml(text: string): string {
    const map: Record<string, string> = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}
