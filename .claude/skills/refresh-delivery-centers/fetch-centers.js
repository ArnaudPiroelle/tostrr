// Runs in a tesla.com/findus page (same origin, so the findus API answers). Starts the collection in the background
// and returns at once; progress is read from window.__dc, and `window.__dc.stop = true` ends it.
// One detail request every DELAY_MS: faster than that, Akamai starts answering 403 after a few hundred calls.
(() => {
  const DELAY_MS = 4000;
  const PAUSE_ON_403_MS = 90000;
  const state = (window.__dc = { status: 'listing', total: 0, done: 0, rows: [], errors: [] });
  const sleep = ms => new Promise(r => setTimeout(r, ms));
  // TSV-safe: the reader splits rows on tabs.
  const clean = v => String(v ?? '').replace(/[\t\r\n]+/g, ' ').trim();

  (async () => {
    // Despite `country=FR`, this returns every location in the world.
    const list = (await (await fetch('/api/findus/get-locations?country=FR&view=map')).json()).data.data;
    const centers = list.filter(l => (l.location_type || []).includes('delivery_center'));
    state.total = centers.length;
    state.status = 'details';

    for (const l of centers) {
      if (state.stop) { state.status = 'stopped'; return; }
      const url = `/api/findus/get-location-details?locationSlug=${encodeURIComponent(l.location_url_slug)}` +
        `&functionTypes=delivery_center&locale=fr_FR&isInHkMoTw=${l.inHkMoTw ?? false}`;
      let r = await fetch(url);
      if (r.status === 403) {
        state.status = 'paused after 403';
        await sleep(PAUSE_ON_403_MS);
        state.status = 'details';
        r = await fetch(url);
      }
      if (r.status === 403) {
        state.status = 'blocked';
        state.errors.push([l.location_url_slug, 403]);
        return;
      }
      if (r.ok) {
        const m = (await r.json()).data;
        const f = (m.functions || []).find(x => x.address) || {};
        const a = f.address || {};
        state.rows.push([
          l.location_url_slug, m.marketing?.display_name, a.country, a.city,
          (a.formatted_address || []).join(', '), f.status, l.latitude, l.longitude,
        ].map(clean).join('\t'));
      } else {
        state.errors.push([l.location_url_slug, r.status]);
      }
      state.done++;
      await sleep(DELAY_MS);
    }
    state.status = 'finished';
  })().catch(e => { state.status = 'failed: ' + e; });

  return 'started';
})();
