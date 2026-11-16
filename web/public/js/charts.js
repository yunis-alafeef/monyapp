import { convertMinor, escapeHtml, formatMinor } from "./money.js";

export function buildMonthlySeries(debts = [], payments = [], targetCurrency = "YER", nowMillis = Date.now(), currencies = []) {
  const baseDate = new Date(nowMillis);
  const series = [];
  for (let offset = 5; offset >= 0; offset--) {
    const startDate = new Date(baseDate.getFullYear(), baseDate.getMonth() - offset, 1, 0, 0, 0, 0);
    const endDate = new Date(startDate.getFullYear(), startDate.getMonth() + 1, 1, 0, 0, 0, 0);
    const start = startDate.getTime();
    const end = endDate.getTime();
    const mm = String(startDate.getMonth() + 1).padStart(2, "0");
    const yy = String(startDate.getFullYear()).slice(-2);

    let receivables = 0;
    let payables = 0;
    for (const d of debts) {
      const t = d.recordedAtMillis || 0;
      if (t >= start && t < end) {
        const amt = convertMinor(d.principalMinor || 0, d.currencyCode || targetCurrency, targetCurrency, currencies);
        if (d.direction === "RECEIVABLE") receivables += amt;
        else payables += amt;
      }
    }

    let collected = 0;
    for (const p of payments) {
      const t = p.paidAtMillis || 0;
      if (t >= start && t < end) {
        collected += convertMinor(p.amountMinor || 0, p.currencyCode || targetCurrency, targetCurrency, currencies);
      }
    }

    series.push({ monthLabel: `${mm}/${yy}`, receivables, payables, collected });
  }
  return series;
}

export function renderMonthlyCashflowChartHtml(series = []) {
  if (!series.length) return "";
  const maxVal = Math.max(1, ...series.flatMap((s) => [s.receivables, s.payables, s.collected]));
  const width = 320;
  const height = 94;
  const groupW = width / series.length;
  const barW = Math.max(7, Math.min(11, groupW * 0.2));

  const bars = series.map((pt, idx) => {
    const cx = groupW * idx + groupW / 2;
    const recH = Math.max(3, (pt.receivables / maxVal) * (height - 10));
    const payH = Math.max(3, (pt.payables / maxVal) * (height - 10));
    const colH = Math.max(3, (pt.collected / maxVal) * (height - 10));
    return `
      <rect x="${(cx - barW * 1.65).toFixed(1)}" y="${(height - recH).toFixed(1)}" width="${barW.toFixed(1)}" height="${recH.toFixed(1)}" rx="3" fill="var(--positive)" />
      <rect x="${(cx - barW * 0.45).toFixed(1)}" y="${(height - payH).toFixed(1)}" width="${barW.toFixed(1)}" height="${payH.toFixed(1)}" rx="3" fill="var(--negative)" />
      <rect x="${(cx + barW * 0.75).toFixed(1)}" y="${(height - colH).toFixed(1)}" width="${barW.toFixed(1)}" height="${colH.toFixed(1)}" rx="3" fill="var(--accent)" />
    `;
  }).join("");

  const labels = series.map((pt) => `<span>${escapeHtml(pt.monthLabel)}</span>`).join("");

  return `
    <div class="card">
      <div style="font-weight:800;font-size:13.5px;margin-bottom:6px;">حركة الديون والتحصيل (آخر 6 أشهر)</div>
      <div class="legend-row">
        <span class="legend-item"><span class="legend-dot" style="background:var(--positive);"></span>مستحق لي</span>
        <span class="legend-item"><span class="legend-dot" style="background:var(--negative);"></span>مستحق عليّ</span>
        <span class="legend-item"><span class="legend-dot" style="background:var(--accent);"></span>المحصل</span>
      </div>
      <svg viewBox="0 0 ${width} ${height}" class="cashflow-svg" preserveAspectRatio="none">${bars}</svg>
      <div class="chart-months">${labels}</div>
    </div>
  `;
}

export function renderReceivablePayableDonutHtml(receivableMinor, payableMinor, currencyCode) {
  const total = receivableMinor + payableMinor;
  const pct = total > 0 ? Math.round((receivableMinor * 100) / total) : 0;
  const r = 32;
  const circ = 2 * Math.PI * r;
  const recLen = total > 0 ? (receivableMinor / total) * circ : 0;

  return `
    <div class="card">
      <div style="font-weight:800;font-size:13.5px;margin-bottom:10px;">مقارنة المستحقات والالتزامات</div>
      <div class="donut-row">
        <div class="donut-wrap">
          <svg viewBox="0 0 84 84" width="82" height="82">
            <circle cx="42" cy="42" r="${r}" fill="none" stroke="rgba(225, 29, 72, 0.22)" stroke-width="11" />
            ${total > 0 ? `
              <circle cx="42" cy="42" r="${r}" fill="none" stroke="var(--negative)" stroke-width="11" />
              <circle cx="42" cy="42" r="${r}" fill="none" stroke="var(--positive)" stroke-width="11"
                stroke-dasharray="${recLen.toFixed(1)} ${circ.toFixed(1)}"
                stroke-linecap="round" transform="rotate(-90 42 42)" />
            ` : ""}
          </svg>
          <div class="donut-center">${pct}%</div>
        </div>
        <div class="donut-legend">
          <div>
            <div class="legend-item"><span class="legend-dot" style="background:var(--positive);"></span>مستحقة لي</div>
            <div style="font-weight:800;font-size:14px;">${formatMinor(receivableMinor, currencyCode)}</div>
          </div>
          <div>
            <div class="legend-item"><span class="legend-dot" style="background:var(--negative);"></span>مستحقة عليّ</div>
            <div style="font-weight:800;font-size:14px;">${formatMinor(payableMinor, currencyCode)}</div>
          </div>
        </div>
      </div>
    </div>
  `;
}
