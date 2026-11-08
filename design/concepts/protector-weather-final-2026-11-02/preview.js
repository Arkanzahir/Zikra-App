(() => {
  const root = document.getElementById('zikra-protector-final');
  const geography = JSON.parse(root.querySelector('#pp-geography').textContent);
  const assets = JSON.parse(root.querySelector('#pp-assets').textContent);
  const state = {
    tema: 'Terang',
    suasana: 'Siang',
    layar: 'Cuaca',
    jam: 0,
    status: 'Tersambung'
  };

  function theme() {
    if (state.layar === 'Cuaca') return 'dark';
    return state.tema === 'Gelap' || (state.tema === 'Ikuti perangkat' && matchMedia('(prefers-color-scheme: dark)').matches) ? 'dark' : 'light';
  }

  function assetImage(el, uri) {
    if (el.dataset.asset === uri) return;
    el.dataset.asset = uri;
    const img = document.createElement('img');
    img.src = uri;
    img.alt = '';
    el.replaceChildren(img);
  }

  function assetIcon(el, name) {
    assetImage(el, assets.icons[theme()][name]);
  }

  function forecast() {
    if (state.suasana === 'Malam') return {
      times: ['20.00', '21.00', '22.00', '23.00', '00.00'],
      temps: [27, 26, 26, 25, 25],
      conditions: ['Berawan', 'Berawan', 'Cerah berawan', 'Cerah berawan'],
      icons: ['cloud', 'cloud', 'cloud-moon', 'cloud-moon']
    };
    if (state.suasana === 'Hujan') return {
      times: ['14.00', '15.00', '16.00', '17.00', '18.00'],
      temps: [28, 27, 27, 27, 26],
      conditions: ['Hujan ringan', 'Hujan ringan', 'Berawan', 'Berawan'],
      icons: ['cloud-rain', 'cloud-rain', 'cloud', 'cloud']
    };
    return {
      times: ['14.00', '15.00', '16.00', '17.00', '18.00'],
      temps: [31, 29, 28, 28, 27],
      conditions: ['Cerah berawan', 'Hujan ringan', 'Hujan ringan', 'Berawan'],
      icons: ['cloud-sun', 'cloud-rain', 'cloud-rain', 'cloud']
    };
  }
  const ns = 'http://www.w3.org/2000/svg';

  function node(tag, attrs) {
    const el = document.createElementNS(ns, tag);
    Object.entries(attrs).forEach(([k, v]) => el.setAttribute(k, String(v)));
    return el;
  }

  function maps() {
    if (!globalThis.d3) return;
    root.querySelectorAll('.pp-map').forEach(svg => {
      const w = svg.getBoundingClientRect().width,
        h = svg.getBoundingClientRect().height;
      if (!w || !h) return;
      svg.replaceChildren();
      svg.setAttribute('viewBox', `0 0 ${w} ${h}`);
      const projection = d3.geoIdentity().reflectY(true).fitExtent([
          [12, 24],
          [w - 12, h - 22]
        ], geography),
        path = d3.geoPath(projection);
      geography.features.forEach(f => svg.appendChild(node('path', {
        d: path(f),
        class: 'pp-land' + (f.properties.is_indonesia ? '' : ' pp-land-neighbor')
      })));
      const points = [
        ['Sumatra', 101.1, -2.2],
        ['Jawa', 110.8, -8.9],
        ['Kalimantan', 114.7, .8],
        ['Sulawesi', 121.1, -5.9],
        ['Papua', 137, -5.8]
      ];
      points.forEach(([label, lon, lat]) => {
        if (w < 340 && label === 'Sulawesi') return;
        const [x, y] = projection([lon, lat]);
        const t = node('text', {
          x,
          y,
          class: 'pp-map-label'
        });
        t.textContent = label;
        svg.appendChild(t);
      });
      const [x, y] = projection([131.38, -.72]);
      const marker = node('g', {
        class: 'pp-map-marker',
        transform: `translate(${x} ${y})`
      });
      marker.append(node('circle', {
        r: 12
      }), node('circle', {
        r: 5
      }));
      svg.appendChild(marker);
    });
  }

  function renderDailyForecast(missing) {
    const rows = state.suasana === 'Hujan' ? [{
      day: 'Hari ini',
      low: 24,
      high: 28,
      icon: 'light-rain'
    }, {
      day: 'Sel',
      low: 24,
      high: 30,
      icon: 'rain'
    }, {
      day: 'Rab',
      low: 25,
      high: 31,
      icon: 'cloudy'
    }] : [{
      day: 'Hari ini',
      low: 26,
      high: 31,
      icon: 'light-rain'
    }, {
      day: 'Sel',
      low: 25,
      high: 32,
      icon: 'light-rain'
    }, {
      day: 'Rab',
      low: 25,
      high: 31,
      icon: 'cloudy'
    }];
    root.querySelector('.wd-daily').hidden = missing;
    const list = root.querySelector('.wd-daily-rows');
    list.replaceChildren();
    const low = Math.min(...rows.map(row => row.low));
    const high = Math.max(...rows.map(row => row.high));
    rows.forEach(row => {
      const element = document.createElement('div');
      element.className = 'wd-daily-row';
      element.setAttribute('role', 'listitem');
      element.setAttribute('aria-label', `${row.day}, minimum ${row.low}, maksimum ${row.high} derajat Celsius, data contoh`);
      const name = document.createElement('span');
      name.textContent = row.day;
      const icon = document.createElement('span');
      icon.className = 'pp-icon';
      assetIcon(icon, row.icon);
      const minimum = document.createElement('span');
      minimum.className = 'wd-daily-low';
      minimum.textContent = row.low + '°';
      const range = document.createElement('span');
      range.className = 'wd-range';
      range.setAttribute('aria-hidden', 'true');
      const mark = document.createElement('span');
      mark.style.left = ((row.low - low) / (high - low) * 100) + '%';
      mark.style.width = ((row.high - row.low) / (high - low) * 100) + '%';
      range.append(mark);
      const maximum = document.createElement('strong');
      maximum.textContent = row.high + '°';
      element.append(name, icon, minimum, range, maximum);
      list.append(element);
    });
  }

  function render() {
    root.style.colorScheme = state.tema === 'Terang' ? 'light' : state.tema === 'Gelap' ? 'dark' : 'light dark';
    root.dataset.weather = state.suasana;
    root.dataset.activeScreen = state.layar;
    const night = state.suasana === 'Malam',
      rain = state.suasana === 'Hujan';
    const missing = state.status === 'Belum tersedia';
    root.style.setProperty('--pp-sky-top', night ? '#112c41' : rain ? '#3c5e73' : '#0785c5');
    root.style.setProperty('--pp-sky-bottom', night ? '#365f6d' : rain ? '#a5bec7' : '#c6e1e4');
    root.style.setProperty('--pp-city-brightness', night ? '.42' : rain ? '.78' : '1');
    root.style.setProperty('--pp-cloud-opacity', night ? '.45' : '.92');
    root.style.setProperty('--wd-top', night ? '#14293f' : rain ? '#4b677d' : '#347ca9');
    root.style.setProperty('--wd-bottom', night ? '#314c69' : rain ? '#8daabd' : '#79b9d9');
    root.querySelector('.pp-sun').style.visibility = rain ? 'hidden' : 'visible';
    root.querySelector('.pp-sun').style.boxShadow = night ? 'inset -14px -5px 0 #28485d' : '0 0 30px 8px #ffe7ab66';
    root.querySelectorAll('[data-temp]').forEach(e => e.textContent = missing ? '—' : night ? '27' : rain ? '28' : '31');
    root.querySelectorAll('[data-condition]').forEach(e => e.textContent = missing ? 'Cuaca belum tersedia' : night ? 'Berawan' : rain ? 'Hujan ringan' : 'Cerah berawan');
    root.querySelectorAll('[data-humidity]').forEach(e => e.textContent = missing ? '—' : rain ? '88%' : '72%');
    root.querySelectorAll('[data-wind]').forEach(e => e.textContent = missing ? '—' : '12 km/jam');
    root.querySelector('[data-weather-hour]').textContent = night ? '19.00 WIB' : '13.00 WIB';
    root.querySelector('.pp-status>span').textContent = night ? '19.00' : '13.00';
    root.querySelectorAll('[data-screen]').forEach(e => e.hidden = e.dataset.screen !== state.layar);
    root.querySelectorAll('[data-preview]').forEach(button => button.setAttribute('aria-pressed', String(button.dataset.preview === (state.layar === 'Gempa' ? 'Protector' : state.layar))));
    root.querySelector('.pp-phone').hidden = state.layar === 'Aset';
    root.querySelector('.pp-asset-gallery').hidden = state.layar !== 'Aset';
    root.querySelector('.pp-hour-rail').hidden = missing;
    root.querySelector('.pp-forecast-empty').hidden = !missing;
    root.querySelector('.pp-chart-section').hidden = missing;
    root.querySelector('.wd-hour-grid').hidden = missing;
    root.querySelector('.wd-hour-selection').hidden = missing;
    root.querySelector('.wd-empty').hidden = !missing;
    root.querySelector('.wd-graph').hidden = missing;
    assetIcon(root.querySelector('.wd-empty-icon'), 'unavailable');
    renderDailyForecast(missing);
    root.querySelector('[data-weather-brief]').textContent = missing ? 'Prakiraan belum dapat dimuat. Periksa koneksi lalu coba lagi.' : night ? 'Langit berawan malam ini, berangsur lebih cerah mulai pukul 22.00.' : rain ? 'Hujan ringan siang ini, berangsur berawan mulai pukul 16.00.' : 'Cerah berawan siang ini. Hujan ringan diperkirakan mulai pukul 15.00.';
    root.querySelector('[data-detail-freshness]').textContent = missing ? 'Belum ada data valid' : state.status === 'Tersimpan' ? 'Data tersimpan · Contoh diambil 12.00 WIB' : 'Contoh diperbarui 12.00 WIB';
    assetIcon(root.querySelector('.pp-empty-icon'), 'unavailable');
    ['home', 'quran', 'ai', 'study', 'profile'].forEach((name, i) => assetImage(root.querySelectorAll('.pp-dock>span .pp-icon')[i], assets.navigation[theme()][name]));
    root.querySelector('.pp-updated').textContent = missing ? 'Belum ada data cuaca yang dapat ditampilkan' : state.status === 'Tersimpan' ? 'Data tersimpan · Contoh diambil 12.00 WIB' : 'Contoh data cuaca diperbarui 12.00 WIB';
    const data = forecast();

    function icon(el, name) {
      const key = {
        'cloud-sun': 'partly-cloudy-day',
        'cloud-moon': 'partly-cloudy-night',
        'cloud': 'cloudy',
        'cloud-rain': 'light-rain'
      } [name] || name;
      assetIcon(el, key);
    }
    root.querySelectorAll('[data-hour]').forEach(e => {
      const i = Number(e.dataset.hour);
      e.setAttribute('aria-pressed', String(i === state.jam));
      e.querySelector('span:first-child').textContent = data.times[i];
      e.querySelector('strong').textContent = data.temps[i] + '°';
      icon(e.querySelector('.pp-hour-icon'), data.icons[i]);
    });
    root.querySelectorAll('.pp-selected-hour').forEach(label => label.textContent = missing ? 'Belum ada data prakiraan' : data.times[state.jam] + ' · ' + data.conditions[state.jam]);
    icon(root.querySelector('.pp-big-weather'), missing ? 'unavailable' : night ? 'cloud-moon' : rain ? 'cloud-rain' : 'cloud-sun');
    icon(root.querySelector('.wd-now-icon'), missing ? 'unavailable' : night ? 'cloud-moon' : rain ? 'cloud-rain' : 'cloud-sun');
    const chart = root.querySelector('.pp-chart'),
      paths = chart.querySelectorAll('path'),
      max = Math.max(...data.temps),
      min = Math.min(...data.temps);
    const chartWidth = chart.getBoundingClientRect().width || 324;
    chart.setAttribute('viewBox', `0 0 ${chartWidth} 180`);
    const xy = data.temps.map((t, i) => [20 + i * (chartWidth - 40) / 4, 40 + (max - t) / Math.max(1, max - min) * 100]);
    const line = xy.map((p, i) => (i ? 'L' : 'M') + p.join(' ')).join(' ');
    paths[0].setAttribute('d', `M20 40H${chartWidth-20} M20 90H${chartWidth-20} M20 140H${chartWidth-20}`);
    paths[1].setAttribute('d', line + ` L${chartWidth-20} 155H20Z`);
    paths[2].setAttribute('d', line);
    chart.querySelectorAll('.pp-chart-points circle').forEach((p, i) => {
      p.setAttribute('cx', xy[i][0]);
      p.setAttribute('cy', xy[i][1]);
    });
    chart.querySelectorAll('.pp-chart-labels text').forEach((t, i) => {
      const at = i % 5;
      t.textContent = i < 5 ? data.temps[i] + '°' : data.times[at];
      t.setAttribute('x', xy[at][0]);
      if (i < 5) t.setAttribute('y', xy[i][1] - 16);
      t.style.display = i >= 5 && chartWidth < 300 && at % 2 ? 'none' : '';
    });
    chart.setAttribute('aria-label', 'Data contoh suhu: ' + data.temps.map((temp, i) => temp + ' derajat pukul ' + data.times[i]).join(', '));
    maps();
  }

  function save() {
    if (window.openai?.setWidgetState) window.openai.setWidgetState({
      modelContent: {
        protectorFinal: {
          acuan: 'Protector sekarang dipertahankan',
          tema: state.tema,
          suasana: state.suasana,
          layar: state.layar,
          status: state.status,
          weatherDetailStyle: 'Langit dan kartu transparan'
        }
      },
      privateContent: {
        ...state
      }
    }).catch(() => {});
  }

  function restore(s) {
    const v = s?.privateContent;
    if (!v) return;
    if (['Terang', 'Gelap', 'Ikuti perangkat'].includes(v.tema)) state.tema = v.tema;
    if (['Siang', 'Malam', 'Hujan'].includes(v.suasana)) state.suasana = v.suasana;
    if (['Protector', 'Cuaca', 'Gempa', 'Aset'].includes(v.layar)) state.layar = v.layar;
    if (['Tersambung', 'Tersimpan', 'Belum tersedia'].includes(v.status)) state.status = v.status;
    if (Number.isInteger(v.jam) && v.jam >= 0 && v.jam < 4) state.jam = v.jam;
    render();
  }
  const weatherNames = {
    'clear-day': 'Cerah siang',
    'clear-night': 'Cerah malam',
    'partly-cloudy-day': 'Cerah berawan',
    'partly-cloudy-night': 'Berawan malam',
    'cloudy': 'Berawan',
    'light-rain': 'Hujan ringan',
    'rain': 'Hujan',
    'thunderstorm': 'Hujan petir',
    'fog': 'Berkabut',
    'unavailable': 'Belum tersedia'
  };
  Object.entries(weatherNames).forEach(([key, label]) => {
    const tile = document.createElement('div');
    tile.className = 'pp-asset-tile';
    const pair = document.createElement('div');
    pair.className = 'pp-asset-pair';
    ['light', 'dark'].forEach(t => {
      const sample = document.createElement('span'),
        img = document.createElement('img');
      img.src = assets.icons[t][key];
      img.alt = label + ', ' + (t === 'light' ? 'terang' : 'gelap');
      sample.append(img);
      pair.append(sample);
    });
    const name = document.createElement('p');
    name.textContent = label;
    const format = document.createElement('small');
    format.textContent = 'SVG · Android XML';
    tile.append(pair, name, format);
    root.querySelector('.pp-asset-grid').append(tile);
  });
  root.querySelectorAll('[data-preview]').forEach(button => button.addEventListener('click', () => {
    state.layar = button.dataset.preview;
    render();
    save();
  }));
  root.querySelectorAll('[data-hour]').forEach(b => b.addEventListener('click', () => {
    state.jam = Number(b.dataset.hour);
    render();
    save();
  }));
  root.querySelectorAll('[data-action]').forEach(b => b.addEventListener('click', () => {
    const a = b.dataset.action;
    if (a === 'refresh') state.status = 'Tersambung';
    else state.layar = a === 'weather' ? 'Cuaca' : a === 'map' ? 'Gempa' : 'Protector';
    render();
    save();
  }));
  render();
  restore(window.openai?.widgetState);
  window.addEventListener('openai:set_globals', e => restore(e.detail?.globals?.widgetState));
  const observer = new ResizeObserver(maps);
  root.querySelectorAll('.pp-map').forEach(svg => observer.observe(svg));
  const chartObserver = new ResizeObserver(() => {
    if (state.layar === 'Cuaca') render();
  });
  chartObserver.observe(root.querySelector('.pp-chart'));
  matchMedia('(prefers-color-scheme: dark)').addEventListener('change', render);
  if (globalThis.Tweak) {
    const tweak = new Tweak({
      container: root,
      onChange: () => {
        render();
        save();
      }
    });
    tweak.addSelect(state, 'tema', {
      label: 'Tema aplikasi',
      options: ['Terang', 'Gelap', 'Ikuti perangkat']
    });
    tweak.addSelect(state, 'suasana', {
      label: 'Contoh cuaca',
      options: ['Siang', 'Malam', 'Hujan']
    });
    tweak.addSelect(state, 'status', {
      label: 'Status prakiraan',
      options: ['Tersambung', 'Tersimpan', 'Belum tersedia']
    });
  }
})();
