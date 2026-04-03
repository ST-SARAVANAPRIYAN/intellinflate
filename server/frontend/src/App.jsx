import { useEffect, useMemo, useRef, useState } from 'react';

const API_URL = window.location.origin;
const LANE_CONFIGS = [
  {
    mode: 'PLATE',
    title: 'Number Plate',
    description: 'Capture the plate first. This is mandatory for user recognition from the database.',
    endpoint: '/api/detect',
    buttonLabel: 'Detect Plate'
  },
  {
    mode: 'SIDE',
    title: 'Side Tyre Crack',
    description: 'After user recognition, scan left and right side tyres for crack regions.',
    endpoint: '/api/analyze-tire',
    buttonLabel: 'Scan Side Tyre',
    fixedMode: 'SIDE'
  },
  {
    mode: 'FRONT',
    title: 'Front Tyre Tread + Alignment',
    description: 'After user recognition, scan front tyre for tread and alignment analysis.',
    endpoint: '/api/analyze-tire',
    buttonLabel: 'Scan Front Tyre',
    fixedMode: 'FRONT'
  }
];

const MAX_ITEMS_PER_LANE = {
  PLATE: 1,
  FRONT: 1,
  SIDE: 2
};

function createLaneState(config) {
  return {
    ...config,
    items: [],
    results: [],
    isAnalyzing: false,
    phase: 'idle',
    status: 'Waiting for images',
    error: null,
    dragActive: false
  };
}

function createInitialLanes() {
  return LANE_CONFIGS.reduce((acc, config) => {
    acc[config.mode] = createLaneState(config);
    return acc;
  }, {});
}

function App() {
  const fileInputRef = useRef(null);
  const pendingLaneRef = useRef('PLATE');
  const pendingSlotIndexRef = useRef(null);
  const selectedItemsRef = useRef([]);
  const [lanes, setLanes] = useState(createInitialLanes);
  const [clock, setClock] = useState(new Date());
  const [health, setHealth] = useState({ state: 'loading', label: 'Checking server...' });

  useEffect(() => {
    const timer = setInterval(() => setClock(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const allItems = useMemo(() => Object.values(lanes).flatMap((lane) => lane.items), [lanes]);
  const allResults = useMemo(() => Object.values(lanes).flatMap((lane) => lane.results), [lanes]);

  useEffect(() => {
    selectedItemsRef.current = allItems;
  }, [allItems]);

  useEffect(() => {
    const updateHealth = async () => {
      try {
        const response = await fetch('/health');
        const data = await response.json();
        setHealth({
          state: response.ok ? 'ok' : 'warn',
          label: response.ok ? 'Server online' : 'Server responded with warnings',
          db: data.db,
          memoryUsers: data.memoryUsers
        });
      } catch (error) {
        setHealth({ state: 'error', label: 'Server offline', error: error.message });
      }
    };

    updateHealth();
    const timer = setInterval(updateHealth, 15000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    return () => {
      selectedItemsRef.current.forEach((item) => URL.revokeObjectURL(item.previewUrl));
    };
  }, []);

  const addFilesToLane = (laneMode, files, targetIndex = null) => {
    const imageFiles = Array.from(files || []).filter((file) => file.type && file.type.startsWith('image/'));
    if (imageFiles.length === 0) return [];

    const laneSnapshot = lanes[laneMode];
    const maxItems = MAX_ITEMS_PER_LANE[laneMode] || 1;
    let items = [...laneSnapshot.items];
    let itemsToAnalyze = [];

    const toLaneItem = (file) => ({
      id: `${file.name}:${file.size}:${file.lastModified}:${Date.now()}`,
      file,
      previewUrl: URL.createObjectURL(file),
      mode: laneMode
    });

    if (laneMode === 'SIDE' && Number.isInteger(targetIndex)) {
      const incoming = imageFiles[0];
      if (!incoming) return [];
      const replacement = toLaneItem(incoming);
      if (items[targetIndex]) {
        URL.revokeObjectURL(items[targetIndex].previewUrl);
        items[targetIndex] = replacement;
      } else {
        while (items.length < targetIndex) {
          items.push(null);
        }
        items[targetIndex] = replacement;
      }
      items = items.filter(Boolean).slice(0, maxItems);
      itemsToAnalyze = [replacement];
    } else if (maxItems === 1) {
      items.forEach((item) => URL.revokeObjectURL(item.previewUrl));
      const replacement = toLaneItem(imageFiles[imageFiles.length - 1]);
      items = [replacement];
      itemsToAnalyze = [replacement];
    } else {
      const incoming = imageFiles.slice(-maxItems).map(toLaneItem);
      const combined = [...items, ...incoming].slice(-maxItems);
      const keep = new Set(combined.map((item) => item.id));
      items.forEach((item) => {
        if (!keep.has(item.id)) {
          URL.revokeObjectURL(item.previewUrl);
        }
      });
      items = combined;
      itemsToAnalyze = incoming;
    }

    setLanes((current) => {
      const lane = current[laneMode];
      const keepIds = new Set(items.map((item) => item.id));
      const filteredResults = lane.results.filter((result) => keepIds.has(result.id));
      return {
        ...current,
        [laneMode]: {
          ...lane,
          items,
          results: filteredResults,
          phase: 'ready',
          status: `${items.length}/${maxItems} image(s) ready`,
          error: null
        }
      };
    });

    return itemsToAnalyze;
  };

  const analyzeLane = async (laneMode, itemsOverride = null) => {
    const lane = lanes[laneMode];
    const itemsToAnalyze = itemsOverride && itemsOverride.length > 0 ? itemsOverride : lane.items;
    if (lane.isAnalyzing || itemsToAnalyze.length === 0) return;
    if (laneMode !== 'PLATE' && !plateDetected) {
      setLanes((current) => ({
        ...current,
        [laneMode]: {
          ...current[laneMode],
          phase: 'blocked',
          error: 'Number plate detection is required before tyre scanning.'
        }
      }));
      return;
    }
    if (laneMode === 'SIDE' && !frontScanReady) {
      setLanes((current) => ({
        ...current,
        [laneMode]: {
          ...current[laneMode],
          phase: 'blocked',
          error: 'Front tyre scan is required before side crack scan.'
        }
      }));
      return;
    }

    setLanes((current) => ({
      ...current,
      [laneMode]: {
        ...current[laneMode],
        isAnalyzing: true,
        phase: 'analyzing',
        status: `Analyzing 0/${itemsToAnalyze.length}...`,
        error: null
      }
    }));

    try {
      const callSingle = async (item) => {
        const formData = new FormData();
        formData.append('image', item.file);
        if (lane.fixedMode) {
          formData.append('mode', lane.fixedMode);
        }

        const response = await fetch(`${API_URL}${lane.endpoint}`, {
          method: 'POST',
          body: formData
        });
        const data = await response.json();
        if (!response.ok || data?.success === false) {
          throw new Error(data.message || data.error || 'Analysis failed');
        }
        return normalizeSingleResult(laneMode, item, data);
      };

      const results = [];
      for (let index = 0; index < itemsToAnalyze.length; index += 1) {
        const item = itemsToAnalyze[index];
        setLanes((current) => ({
          ...current,
          [laneMode]: {
            ...current[laneMode],
            status: `Analyzing ${index + 1}/${itemsToAnalyze.length}...`
          }
        }));
        // eslint-disable-next-line no-await-in-loop
        const result = await callSingle(item);
        results.push(result);
      }

      setLanes((current) => {
        const plateSuccess = laneMode === 'PLATE' && results.some((result) => result.success);
        const frontSuccess = laneMode === 'FRONT' && results.some((result) => result.success);

        const next = {
          ...current,
          [laneMode]: {
            ...current[laneMode],
            results: [
              ...current[laneMode].results.filter((entry) => !results.some((result) => result.id === entry.id)),
              ...results
            ],
            isAnalyzing: false,
            phase: results.every((item) => item.success) ? 'done' : 'warning',
            status: `${results.filter((item) => item.success).length}/${results.length} scan(s) completed`,
            error: null
          }
        };

        if (plateSuccess && laneMode !== 'FRONT') {
          next.FRONT = {
            ...current.FRONT,
            phase: current.FRONT.phase === 'idle' || current.FRONT.phase === 'blocked' ? 'ready' : current.FRONT.phase,
            status:
              current.FRONT.phase === 'idle' || current.FRONT.phase === 'blocked'
                ? current.FRONT.items.length > 0
                  ? current.FRONT.status
                  : 'Step 2 unlocked: Upload front tyre image'
                : current.FRONT.status,
            error: current.FRONT.phase === 'blocked' ? null : current.FRONT.error
          };
        }

        if (frontSuccess && laneMode !== 'SIDE') {
          next.SIDE = {
            ...current.SIDE,
            phase: current.SIDE.phase === 'idle' || current.SIDE.phase === 'blocked' ? 'ready' : current.SIDE.phase,
            status:
              current.SIDE.phase === 'idle' || current.SIDE.phase === 'blocked'
                ? current.SIDE.items.length > 0
                  ? current.SIDE.status
                  : 'Step 3 unlocked: Upload side tyre image'
                : current.SIDE.status,
            error: current.SIDE.phase === 'blocked' ? null : current.SIDE.error
          };
        }

        return next;
      });
    } catch (error) {
      setLanes((current) => ({
        ...current,
        [laneMode]: {
          ...current[laneMode],
          isAnalyzing: false,
          phase: 'error',
          status: 'Analysis failed',
          error: error.message
        }
      }));
    }
  };

  const openPickerForLane = (laneMode, targetIndex = null) => {
    pendingLaneRef.current = laneMode;
    pendingSlotIndexRef.current = targetIndex;
    fileInputRef.current?.click();
  };

  const onFileSelect = (event) => {
    const laneMode = pendingLaneRef.current;
    const targetIndex = pendingSlotIndexRef.current;
    const itemsToAnalyze = addFilesToLane(laneMode, event.target.files, targetIndex);
    if (itemsToAnalyze.length > 0) {
      analyzeLane(laneMode, itemsToAnalyze);
    }
    pendingSlotIndexRef.current = null;
    event.target.value = '';
  };

  const onDropToLane = (laneMode, event, targetIndex = null) => {
    event.preventDefault();
    if (laneMode !== 'PLATE' && !plateDetected) {
      setLanes((current) => ({
        ...current,
        [laneMode]: {
          ...current[laneMode],
          dragActive: false,
          phase: 'blocked',
          error: 'Drop blocked. Detect number plate first.'
        }
      }));
      return;
    }
    if (laneMode === 'SIDE' && !frontScanReady) {
      setLanes((current) => ({
        ...current,
        [laneMode]: {
          ...current[laneMode],
          dragActive: false,
          phase: 'blocked',
          error: 'Drop blocked. Front tyre scan first, then side crack scan.'
        }
      }));
      return;
    }
    const itemsToAnalyze = addFilesToLane(laneMode, event.dataTransfer.files, targetIndex);
    if (itemsToAnalyze.length > 0) {
      analyzeLane(laneMode, itemsToAnalyze);
    }
    setLanes((current) => ({
      ...current,
      [laneMode]: {
        ...current[laneMode],
        dragActive: false
      }
    }));
  };

  const totalSelected = allItems.length;
  const totalResults = allResults.length;
  const successfulResults = allResults.filter((item) => item.success).length;
  const frontPreview = lanes.FRONT.items[0]?.previewUrl;
  const platePreview = lanes.PLATE.items[0]?.previewUrl;
  const sidePreviewOne = lanes.SIDE.items[0]?.previewUrl;
  const sidePreviewTwo = lanes.SIDE.items[1]?.previewUrl;
  const plateResult = lanes.PLATE.results.find((result) => result.success);
  const recognizedUser = plateResult?.user || null;
  const plateDetected = Boolean(plateResult?.detectedPlate || plateResult?.text);
  const frontScanReady = lanes.FRONT.results.some((result) => result.success);
  const plateGateMessage = !plateDetected
    ? 'Step 1: Detect number plate and find user first.'
    : !frontScanReady
      ? 'Step 2: Upload front tyre image to detect tread wear and misalignment.'
      : 'Step 3: Upload side tyre image(s) for crack detection.';
  const workflowError = [lanes.PLATE.error, lanes.FRONT.error, lanes.SIDE.error].find(Boolean) || null;

  return (
    <div className="app-shell">
      <div className="orb orb-one" />
      <div className="orb orb-two" />

      <main className="workspace">
        <header className="hero panel-surface">
          <div className="hero-copy">
            <span className="eyebrow">Vehicle Intake Workflow</span>
            <h1>IntellInflate Vehicle Scan Console</h1>
            <p>
              Plate detection is mandatory. Once plate is detected, the system recognizes the user from the database and unlocks tyre scans.
            </p>
            <div className="hero-chips">
              <span className="chip chip-accent">Number Plate First</span>
              <span className="chip">DB User Recognition</span>
              <span className="chip">Tyre Scan Workflow</span>
            </div>
          </div>

          <div className="hero-actions">
            <div className={`status-card status-${health.state}`}>
              <span className="status-label">Backend</span>
              <strong>{health.label}</strong>
              <small>{health.db ? `DB: ${health.db}` : 'Same-origin API'}</small>
            </div>
            <div className="action-row">
              <span className="summary-pill">{totalSelected} selected</span>
              <span className="summary-pill">{successfulResults}/{totalResults || 0} scanned</span>
              <span className="summary-pill mono">{clock.toLocaleTimeString([], { hour12: false })}</span>
            </div>
          </div>
        </header>

        <section className="stats-grid">
          <article className="stat-card">
            <span>Number Plate</span>
            <strong>{lanes.PLATE.items.length}</strong>
            <small>Plate capture ready</small>
          </article>
          <article className="stat-card">
            <span>Side Tyre</span>
            <strong>{lanes.SIDE.items.length}</strong>
            <small>Left and right slots</small>
          </article>
          <article className="stat-card">
            <span>Front Tyre</span>
            <strong>{lanes.FRONT.items.length}</strong>
            <small>Front scan slot</small>
          </article>
          <article className="stat-card">
            <span>User Recognition</span>
            <strong>{recognizedUser ? 'MATCHED' : plateDetected ? 'NOT FOUND' : 'WAITING'}</strong>
            <small>{plateDetected ? 'Plate scanned' : 'Detect plate to continue'}</small>
          </article>
        </section>

        <section className="vehicle-collage panel-surface">
          <div className="collage-head">
            <h2>Whole Vehicle Capture Grid</h2>
            <p>Upload image and detection starts immediately. Number plate is mandatory before tyre scanning.</p>
          </div>

          <div className="collage-grid">
            <article
              className={`capture-slot slot-front ${lanes.FRONT.dragActive ? 'drag-active' : ''}`}
              onDragEnter={(event) => {
                event.preventDefault();
                setLanes((current) => ({
                  ...current,
                  FRONT: {
                    ...current.FRONT,
                    dragActive: true
                  }
                }));
              }}
              onDragOver={(event) => {
                event.preventDefault();
              }}
              onDragLeave={() => {
                setLanes((current) => ({
                  ...current,
                  FRONT: {
                    ...current.FRONT,
                    dragActive: false
                  }
                }));
              }}
              onDrop={(event) => onDropToLane('FRONT', event)}
            >
              <header>
                <strong>Front Tyre</strong>
                <span>{lanes.FRONT.items.length} image(s)</span>
              </header>
              <div className={`lane-live-status lane-${lanes.FRONT.phase}`}>
                <span className="lane-dot" />
                <small>{lanes.FRONT.status}</small>
              </div>
              {frontPreview ? <img src={frontPreview} alt="Front tyre preview" /> : <p>Drop a tall front tyre image</p>}
              <div className="slot-actions">
                <button type="button" className="btn btn-secondary" onClick={() => openPickerForLane('FRONT')} disabled={!plateDetected || lanes.FRONT.isAnalyzing}>
                  {lanes.FRONT.isAnalyzing ? 'Scanning...' : 'Upload Front Tyre'}
                </button>
              </div>
            </article>

            <article
              className={`capture-slot slot-plate ${lanes.PLATE.dragActive ? 'drag-active' : ''}`}
              onDragEnter={(event) => {
                event.preventDefault();
                setLanes((current) => ({
                  ...current,
                  PLATE: {
                    ...current.PLATE,
                    dragActive: true
                  }
                }));
              }}
              onDragOver={(event) => {
                event.preventDefault();
              }}
              onDragLeave={() => {
                setLanes((current) => ({
                  ...current,
                  PLATE: {
                    ...current.PLATE,
                    dragActive: false
                  }
                }));
              }}
              onDrop={(event) => onDropToLane('PLATE', event)}
            >
              <header>
                <strong>Number Plate</strong>
                <span>{lanes.PLATE.items.length} image(s)</span>
              </header>
              <div className={`lane-live-status lane-${lanes.PLATE.phase}`}>
                <span className="lane-dot" />
                <small>{lanes.PLATE.status}</small>
              </div>
              {platePreview ? <img src={platePreview} alt="Number plate preview" /> : <p>Drop a front-facing plate image</p>}
              <div className="slot-actions">
                <button type="button" className="btn btn-primary" onClick={() => openPickerForLane('PLATE')} disabled={lanes.PLATE.isAnalyzing}>
                  {lanes.PLATE.isAnalyzing ? 'Detecting...' : 'Upload Number Plate'}
                </button>
              </div>
            </article>

            <article
              className={`capture-slot slot-side ${lanes.SIDE.dragActive ? 'drag-active' : ''}`}
              onDragEnter={(event) => {
                event.preventDefault();
                setLanes((current) => ({
                  ...current,
                  SIDE: {
                    ...current.SIDE,
                    dragActive: true
                  }
                }));
              }}
              onDragOver={(event) => {
                event.preventDefault();
              }}
              onDragLeave={() => {
                setLanes((current) => ({
                  ...current,
                  SIDE: {
                    ...current.SIDE,
                    dragActive: false
                  }
                }));
              }}
              onDrop={(event) => onDropToLane('SIDE', event, 0)}
            >
              <header>
                <strong>Side Tyre - Left</strong>
                <span>{sidePreviewOne ? 'Ready' : 'Empty'}</span>
              </header>
              <div className={`lane-live-status lane-${lanes.SIDE.phase}`}>
                <span className="lane-dot" />
                <small>{lanes.SIDE.status}</small>
              </div>
              {sidePreviewOne ? <img src={sidePreviewOne} alt="Side tyre left preview" /> : <p>Drop left-side tyre image</p>}
              <div className="slot-actions">
                <button type="button" className="btn btn-secondary" onClick={() => openPickerForLane('SIDE', 0)} disabled={!frontScanReady || lanes.SIDE.isAnalyzing}>
                  {lanes.SIDE.isAnalyzing ? 'Scanning...' : 'Upload Left Side Tyre'}
                </button>
              </div>
            </article>

            <article
              className={`capture-slot slot-side ${lanes.SIDE.dragActive ? 'drag-active' : ''}`}
              onDragEnter={(event) => {
                event.preventDefault();
                setLanes((current) => ({
                  ...current,
                  SIDE: {
                    ...current.SIDE,
                    dragActive: true
                  }
                }));
              }}
              onDragOver={(event) => {
                event.preventDefault();
              }}
              onDragLeave={() => {
                setLanes((current) => ({
                  ...current,
                  SIDE: {
                    ...current.SIDE,
                    dragActive: false
                  }
                }));
              }}
              onDrop={(event) => onDropToLane('SIDE', event, 1)}
            >
              <header>
                <strong>Side Tyre - Right</strong>
                <span>{sidePreviewTwo ? 'Ready' : 'Empty'}</span>
              </header>
              <div className={`lane-live-status lane-${lanes.SIDE.phase}`}>
                <span className="lane-dot" />
                <small>{lanes.SIDE.status}</small>
              </div>
              {sidePreviewTwo ? <img src={sidePreviewTwo} alt="Side tyre right preview" /> : <p>Drop right-side tyre image</p>}
              <div className="slot-actions">
                <button type="button" className="btn btn-secondary" onClick={() => openPickerForLane('SIDE', 1)} disabled={!frontScanReady || lanes.SIDE.isAnalyzing}>
                  {lanes.SIDE.isAnalyzing ? 'Scanning...' : 'Upload Right Side Tyre'}
                </button>
              </div>
            </article>
          </div>
          {plateGateMessage ? <div className="workflow-warning">{plateGateMessage}</div> : null}
          {workflowError ? <div className="lane-error">{workflowError}</div> : null}
        </section>

        <section className="identity-panel panel-surface">
          <div className="identity-head">
            <h2>User Recognition</h2>
            <span className={`status-pill ${recognizedUser ? 'pill-success' : plateDetected ? 'pill-warning' : ''}`}>
              {recognizedUser ? 'Recognized' : plateDetected ? 'Plate detected - no DB match' : 'Waiting for plate'}
            </span>
          </div>
          {plateResult ? (
            <div className="identity-body">
              <div className="identity-block">
                <span>Detected Plate</span>
                <strong>{plateResult.detectedPlate || plateResult.text || 'N/A'}</strong>
                <small>{resultMeta(plateResult)}</small>
              </div>
              {recognizedUser ? (
                <div className="identity-block">
                  <span>User Details</span>
                  <strong>{recognizedUser.username}</strong>
                  <small>Email: {recognizedUser.email || 'N/A'}</small>
                  <small>Vehicle: {recognizedUser.vehicleModel || 'N/A'}</small>
                  <small>Phone: {recognizedUser.phone || 'N/A'}</small>
                </div>
              ) : (
                <div className="identity-block">
                  <span>User Details</span>
                  <strong>Not found in database</strong>
                  <small>Register this plate to auto-link vehicle scans to a user.</small>
                </div>
              )}
            </div>
          ) : (
            <div className="empty-state">Detect number plate to recognize user and continue tyre scanning.</div>
          )}
        </section>

        <section className="result-grid workflow-results">
          {[...lanes.PLATE.results, ...lanes.FRONT.results, ...lanes.SIDE.results].length === 0 ? (
            <div className="empty-state">Results will appear here after each workflow step.</div>
          ) : (
            [...lanes.PLATE.results, ...lanes.FRONT.results, ...lanes.SIDE.results].map((result) => (
              <article className={`result-card ${result.success ? 'ok' : 'fail'}`} key={result.id}>
                <div className="preview-frame">
                  <img src={result.previewUrl} alt={result.filename} />
                  <div className="overlay-layer">{renderOverlays(result)}</div>
                </div>
                <div className="card-body result-body">
                  <div className="result-row">
                    <span className="mode-pill">{formatMode(result.mode)}</span>
                    <span className="card-name" title={result.filename}>
                      {result.filename}
                    </span>
                  </div>
                  <div className="result-title">{resultTitle(result)}</div>
                  <div className="result-meta">{resultMeta(result)}</div>
                </div>
              </article>
            ))
          )}
        </section>
      </main>

      <input ref={fileInputRef} type="file" accept="image/*" hidden onChange={onFileSelect} />
    </div>
  );
}

function normalizeSingleResult(laneMode, item, result) {
  return {
    id: item?.id || `${laneMode}-${Date.now()}`,
    filename: item?.file?.name || result.filename || 'image',
    previewUrl: item?.previewUrl,
    mode: laneMode,
    success: result.success !== false,
    message: result.message || result.error || '',
    ...result
  };
}

function renderOverlays(result) {
  if (!result.success) return null;

  const payload = result.payload || result;

  if (result.mode === 'PLATE') {
    const plateBox = toPercentBox(payload.bbox, payload.imageSize);
    if (!plateBox) return null;
    return <div className="overlay-box overlay-plate" style={boxStyle(plateBox)} />;
  }

  if (result.mode === 'SIDE') {
    const cracks = payload?.cracks?.visualDetections?.crackRegions || payload?.visualDetections?.crackRegions || [];
    const wearRegions = payload?.cracks?.visualDetections?.wearRegions || payload?.visualDetections?.wearRegions || [];
    const imageSize = payload?.cracks?.imageSize || payload?.imageSize;
    const overlays = [];

    cracks.forEach((region, index) => {
      const box = toPercentBox(region, imageSize);
      if (!box) return;
      overlays.push(<div key={`${result.id}-crack-${index}`} className="overlay-box overlay-crack" style={boxStyle(box)} />);
    });

    wearRegions.forEach((region, index) => {
      const box = toPercentBox(region, imageSize);
      if (!box) return;
      overlays.push(<div key={`${result.id}-wear-${index}`} className="overlay-box overlay-wear" style={boxStyle(box)} />);
    });

    return overlays;
  }

  if (result.mode === 'FRONT') {
    const visual = payload?.visualDetections || {};
    const imageSize = payload?.imageSize;
    const overlays = [];

    const treadBox = toPercentBox(visual.treadRegion, imageSize);
    if (treadBox) {
      overlays.push(<div key={`${result.id}-tread`} className="overlay-box overlay-tread" style={boxStyle(treadBox)} />);
    }

    (visual.wearRegions || []).forEach((region, index) => {
      const box = toPercentBox(region, imageSize);
      if (!box) return;
      overlays.push(
        <div
          key={`${result.id}-wear-${index}`}
          className={`overlay-box overlay-wear ${Number(region.severity === 'High') ? 'overlay-primary' : ''}`}
          style={boxStyle(box)}
        />
      );
    });

    const dominantIndex = Number(visual.dominantStripIndex);
    (visual.alignmentRegions || []).forEach((region) => {
      const box = toPercentBox(region, imageSize);
      if (!box) return;
      const primary = Number(region.index) === dominantIndex;
      overlays.push(
        <div
          key={`${result.id}-strip-${region.index}`}
          className={`overlay-box overlay-alignment ${primary ? 'overlay-primary' : ''}`}
          style={boxStyle(box)}
        />
      );
    });

    return overlays;
  }

  return null;
}

function boxStyle(box) {
  return {
    left: `${box.left}%`,
    top: `${box.top}%`,
    width: `${box.width}%`,
    height: `${box.height}%`
  };
}

function toPercentBox(box, imageSize) {
  if (!box || !imageSize || !imageSize.width || !imageSize.height) return null;
  const left = (Number(box.x || 0) / Number(imageSize.width)) * 100;
  const top = (Number(box.y || 0) / Number(imageSize.height)) * 100;
  const width = (Number(box.w || 0) / Number(imageSize.width)) * 100;
  const height = (Number(box.h || 0) / Number(imageSize.height)) * 100;
  if (width <= 0 || height <= 0) return null;

  return {
    left: clamp(left, 0, 100),
    top: clamp(top, 0, 100),
    width: clamp(width, 0, 100),
    height: clamp(height, 0, 100)
  };
}

function resultTitle(result) {
  if (!result.success) return 'Detection failed';

  const payload = result.payload || result;
  if (result.mode === 'PLATE') {
    return payload.detectedPlate || payload.text || 'Plate detected';
  }
  if (result.mode === 'SIDE') {
    const crackStatus = payload?.cracks?.status || payload.status || 'Crack analysis complete';
    const wearStatus = payload?.cracks?.wear?.status || payload?.wear?.status || '';
    return wearStatus ? `${crackStatus} | ${wearStatus}` : crackStatus;
  }
  return `${payload?.tread?.status || 'Tread'} | ${payload?.wear?.status || 'Wear'} | ${payload?.alignment?.status || 'Alignment'}`;
}

function resultMeta(result) {
  if (!result.success) {
    return `Error: ${result.message || 'Unknown issue'}`;
  }

  const payload = result.payload || result;
  const source = payload.modelSource || payload?.cracks?.modelSource || payload?.tread?.modelSource || payload?.alignment?.modelSource;
  const fallbackReason = payload.fallbackReason || payload?.cracks?.fallbackReason || payload?.tread?.fallbackReason || payload?.alignment?.fallbackReason;
  if (result.mode === 'PLATE') {
    const conf = Math.round((payload.confidence || 0) * 100);
    return `Confidence ${conf}% | OCR ${payload.ocrEngine || 'easyocr'}${source ? ` | Source ${source}` : ''}${fallbackReason ? ` | Fallback ${fallbackReason}` : ''}`;
  }
  if (result.mode === 'SIDE') {
    const cracks = payload?.cracks || payload;
    const wear = payload?.wear || payload?.cracks?.wear || null;
    const wearText = wear ? ` | Wear ${wear.status || 'n/a'} ${wear.value || ''}` : '';
    return `Cracks ${cracks.count || 0} | Severity ${cracks.status || 'n/a'}${wearText}${source ? ` | Source ${source}` : ''}${fallbackReason ? ` | Fallback ${fallbackReason}` : ''}`;
  }
  return `Tread ${payload?.tread?.value || '--'} | Wear ${payload?.wear?.value || '--'} | Alignment ${payload?.alignment?.value || '--'} | Score ${payload.overall_score ?? '--'}${source ? ` | Source ${source}` : ''}${fallbackReason ? ` | Fallback ${fallbackReason}` : ''}`;
}

function formatMode(mode) {
  if (mode === 'PLATE') return 'Number Plate';
  if (mode === 'SIDE') return 'Side Tyre';
  if (mode === 'FRONT') return 'Front Tyre';
  return mode;
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}

export default App;