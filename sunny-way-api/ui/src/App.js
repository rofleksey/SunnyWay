import './App.css';
import { isDev } from './lib'
import axios from 'axios';
import { Canvas } from 'leaflet';
import { Pane, MapContainer, MapConsumer, TileLayer, Marker, Polyline, Popup, useMapEvents } from 'react-leaflet';
import { debounce } from 'lodash';
import React, { useState, useEffect, useLayoutEffect, useMemo, useRef } from 'react';
import { Chart } from 'react-charts';
import { BrowserView, MobileView, isBrowser, isMobile } from 'react-device-detect';
import LoadingBar from 'react-top-loading-bar';

import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Slider from '@mui/material/Slider';
import FormControlLabel from '@mui/material/FormControlLabel';
import Switch from '@mui/material/Switch';
import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';
import AdapterMoment from '@mui/lab/AdapterMoment';
import DateTimePicker from '@mui/lab/DateTimePicker';
import LocalizationProvider from '@mui/lab/LocalizationProvider';
import Tooltip from '@mui/material/Tooltip';

import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

const SHADE_CHUNK_SIZE = 1000;
const SHADE_UPDATE_INTERVAL = 33;
const SUN_MARGIN = 20;
const SUN_SIZE = 48;
const DEFAULT_FACTOR = 0.75;
let shadeUpdateTimeout;

// #ffb703 #fb8500 #219ebc #023047
function factorToColor(factor) {
  let color;
  if (factor >= 9) {
    color = '#fb8500'
  } else if (factor >= 5) {
    color = '#219ebc';
  } else if (factor >= 1) {
    color = '#023047';
  } else {
    color = '#293241';
  }
  return color;
}

function mapFactor(factor) {
  let actualFactor;
  if (factor <= 0.5) {
    actualFactor = 1.1 + 0.9 * Math.pow(factor * 2, 2);
  } else {
    actualFactor = 2 + 8 * Math.pow(2 * (factor - 0.5), 1.5);
  }
  console.log(`input = ${factor}, factor = ${actualFactor}`);
  return actualFactor;
}

function postPath(
  markers,
  curTime,
  maxFactor,
  preferShadow,
  algorithm,
  property,
  setPath,
  setNetworkState
) {
	console.log(markers);
	axios.post("/api/nav", {
		from: {
			lat: markers.startMarker[0],
			lon: markers.startMarker[1]
		},
		to: {
			lat: markers.endMarker[0],
			lon: markers.endMarker[1]
		},
		curTime,
		maxFactor: mapFactor(maxFactor),
		preferShadow,
		algorithm,
	}).then((res) => {
		const array = [];
		res.data.result.path.forEach((edge) => {
			const {fromPoint, toPoint, edgeId, factor} = edge;
			let content = [];
			content.push(`distance = ${edge.distance.toFixed(2)}`);
			content.push(`time to walk (minutes) = ${(edge.time / 60000).toFixed(2)}`);
			if (edge.factor > 0) {
				content.push(`factor = ${factor.toFixed(2)}`);
			}
			array.push({
				id: edgeId,
				positions: [[fromPoint.lat, fromPoint.lon], [toPoint.lat, toPoint.lon]],
				color: factorToColor(edge.factor),
				factor: edge.factor,
				time: edge.time,
				distance: edge.distance,
				content,
			});
		});
		console.log(array);
		setPath(array);
		setNetworkState((oldState) => ({
			...oldState,
			[property]: true,
		}));
	}).catch((e) => {
	  console.error(e);
		const errStr = e?.response?.data?.message ?? e.toString();
		setNetworkState((oldState) => ({
			...oldState,
			error: errStr,
			shortestDone: true,
			secondDone: true,
		}));
	});
}

function updateShadeChunk(shadeArray, shadeChunks, shadeChunkIndex, setActualShade) {
  console.log(`Adding chunk #${shadeChunkIndex}`)
  const mapped = shadeChunks[shadeChunkIndex++].map(({fromPoint, toPoint, factor}) => {
     return {color: factorToColor(factor), positions: [[fromPoint.lat, fromPoint.lon], [toPoint.lat, toPoint.lon]]};
  });
  shadeArray.push(mapped);
  setActualShade([...shadeArray]);
  if (shadeChunkIndex < shadeChunks.length) {
    shadeUpdateTimeout = setTimeout(() => {
      updateShadeChunk(shadeArray, shadeChunks, shadeChunkIndex, setActualShade);
    }, SHADE_UPDATE_INTERVAL);
  }
}

function postShade(centerLat, centerLon, radius, curTime, setShade) {
	axios.post("/api/shadow-map", {
		center: {
			lat: centerLat,
			lon: centerLon
		},
		radius,
		time: +Date.parse(curTime),
	}).then((res) => {
		const array = res.data.edges;
		clearTimeout(shadeUpdateTimeout);
		if (array.length === 0) {
		  setShade([]);
		  return;
		}
		let shadeChunks = [];
    for (let i = 0; i < array.length; i += SHADE_CHUNK_SIZE) {
        shadeChunks.push(array.slice(i, i + SHADE_CHUNK_SIZE))
    }
    setShade(shadeChunks);
	}).catch((e) => {
	  setShade([]);
		console.error(e);
	});
}

function fetchSun(map, curTime, setSun) {
  const center = map.getCenter();
	axios.post("/api/sun", {
	  center: { lat: center.lat, lon: center.lng },
		curTime: +Date.parse(curTime),
	}).then(({data}) => {
		setSun(data);
	}).catch((e) => {
	  setSun(null);
		console.error(e);
	});
}

function fetchShade(map, curTime, setShade) {
    const center = map.getCenter();
    const mapBoundNorthEast = map.getBounds().getNorthEast();
    const mapDistance = mapBoundNorthEast.distanceTo(center);
    const radius = mapDistance;
    console.log(center, radius);
    postShade(center.lat, center.lng, radius, curTime, setShade);
}

function MapListener({markers, setMarkers, networkState, setNetworkState, setShortestPath, setSecondPath, setShade, setSun, curTime}) {
	const map = useMapEvents({
	    moveend: (e) => {
	        fetchShade(map, curTime, setShade);
	        fetchSun(map, curTime, setSun);
	    },
	    click: (e) => {
            if (markers.startMarker && markers.endMarker && (!networkState.shortestDone || !networkState.secondDone) && !networkState.error) {
                return;
            }
            if (!markers.startMarker || markers.endMarker) {
                setMarkers((oldMarkers) => ({
                    ...oldMarkers,
                    startMarker: [e.latlng.lat, e.latlng.lng],
                    endMarker: null,
                }));
                setShortestPath([]);
                setSecondPath([]);
                setNetworkState((oldState) => ({
                    pending: false,
                    error: null,
                    shortestDone: false,
                    secondDone: false,
                }));
            } else {
                setMarkers((oldMarkers) => ({
                    ...oldMarkers,
                    endMarker: [e.latlng.lat, e.latlng.lng],
                }));
                setNetworkState((oldState) => ({
                    ...oldState,
                    rebuildPath: true,
                }));
                console.log("!");
            }
        }
	});
	return null;
}

function fetchPaths(
  markers,
  setMarkers,
  prevMarkers,
  setPrevMarkers,
  setNetworkState,
  setShortestPath,
  setSecondPath,
  curTime,
  maxFactor,
  preferShadow
) {
	const restTime = +Date.parse(curTime);
	console.log(markers, prevMarkers);
	const curMarkers = markers.endMarker !== null ? markers : prevMarkers;
	setNetworkState((oldState) => ({
		...oldState,
		rebuildPath: false,
		shortestDone: false,
		secondDone: false,
		error: null,
		pending: true,
	}));
	postPath(curMarkers, restTime, maxFactor, preferShadow, 'distance', 'shortestDone', setShortestPath, setNetworkState);
	postPath(curMarkers, restTime, maxFactor, preferShadow, 'shadow', 'secondDone', setSecondPath, setNetworkState);
	setPrevMarkers((oldState) => ({
		...oldState,
		startMarker: curMarkers.startMarker,
		endMarker: curMarkers.endMarker,
	}));
	setMarkers((oldMarkers) => ({
		...oldMarkers,
		startMarker: null,
		endMarker: null,
	}));
}

function PathPolyline({id, color, positions, content, defaultColor}) {
  const actualColor = defaultColor ? '#ff002b' : color;
  const actualWeight = defaultColor ? 5 : 7;
  return <Polyline
    key={id}
    onMouseOver={(e) => {
      e.target.openPopup();
    }}
    onMouseOut={(e) => {
      e.target.closePopup();
    }}
    pathOptions={{color: actualColor, weight: actualWeight}}
    positions={positions}>
    <Popup>
      <p>
      {content.map((s) => ([s, <br key="br"/>]))}
      </p>
    </Popup>
  </Polyline>
}

function PathComponent({ path, defaultColor }) {
	return path.map(({id, positions, color, content}) => (
		<PathPolyline
		  id={id}
		  color={color}
		  positions={positions}
		  content={content}
		  defaultColor={defaultColor} />
	))
}

function ShadePolyline({id, color, opacity, positions}) {
  return <Polyline
    key={id}
    interactive={false}
    onMouseOver={(e) => {
      e.target.openPopup();
    }}
    onMouseOut={(e) => {
      e.target.closePopup();
    }}
    pathOptions={{
        color,
        weight: 4,
        opacity,
        dashArray: "10 10"
     }}
    positions={positions}>
  </Polyline>
}

function ShadeComponent({ path, opacity }) {
	return path.map(({id, positions, color}) => (
		<ShadePolyline
		 id={id}
		 color={color}
		 positions={positions}
		 opacity={opacity} />
	))
}

const MemoPathComponent = React.memo(PathComponent);
const MemoShadeComponent = React.memo(ShadeComponent);

function ChartComponent({ chartData }) {
  return <Chart
    options={{
      data: chartData,
      defaultColors: [ '#ff002b', '#fb8500' ],
      padding: 25,
      tooltip: false,
      primaryCursor: false,
      secondaryCursor: false,
      showVoronoi: false,
      showDebugAxes: false,
      useIntersectionObserver: false,
      primaryAxis: {
        getValue: (data) => data.time
      },
      secondaryAxes: [{
        getValue: (data) => data.sunCost,
        show: false,
      }],
    }}
  />
}

const MemoChartComponent = React.memo(ChartComponent);

function StatisticsComponent({ data }) {
  const [shortestStats, secondStats] = data;
  const shortestDistance = Math.round(shortestStats.totalDistance);
  const secondDistance = Math.round(secondStats.totalDistance);
  const distanceDiff = Math.round(100 * (secondStats.totalDistance / shortestStats.totalDistance - 1));
  let distanceDiffStr;
  if (distanceDiff > 0) {
    distanceDiffStr = ` +${distanceDiff}% `;
  } else if (distanceDiff < 0) {
    distanceDiffStr = ` -${Math.abs(distanceDiff)}% `;
  } else {
    distanceDiffStr = ' = ';
  }
  const shortestSunlight = Math.round(100 * shortestStats.sunlightDistance / shortestStats.totalDistance);
  const secondSunlight = Math.round(100 * secondStats.sunlightDistance / secondStats.totalDistance);
  const sunlightDiff = Math.round(secondSunlight - shortestSunlight);
  let sunlightDiffStr;
  if (sunlightDiff > 0) {
    sunlightDiffStr = ` +${sunlightDiff}% `;
  } else if (sunlightDiff < 0) {
    sunlightDiffStr = ` -${Math.abs(sunlightDiff)}% `;
  } else {
    sunlightDiffStr = ' = ';
  }
  return <>
    <Typography variant="body2" gutterBottom>
      Distance:
      {' '}
      <span style={{color: '#ff002b'}}>
        {shortestDistance}m
      </span>
      {' '}
      <span style={{color: '#fb8500'}}>
        {secondDistance}m ({distanceDiffStr})
      </span>
    </Typography>
    <Typography variant="body2" gutterBottom>
      Direct sun light:
      {' '}
      <span style={{color: '#ff002b'}}>
        {shortestSunlight}%
      </span>
      {' '}
      <span style={{color: '#fb8500'}}>
        {secondSunlight}% ({sunlightDiffStr})
      </span>
    </Typography>
  </>
}

const MemoStatisticsComponent = React.memo(StatisticsComponent);

function useWindowSize() {
  const [size, setSize] = useState([0, 0]);
  useLayoutEffect(() => {
    function updateSize() {
      setSize([window.innerWidth, window.innerHeight]);
    }
    window.addEventListener('resize', updateSize);
    updateSize();
    return () => window.removeEventListener('resize', updateSize);
  }, []);
  return size;
}

function App() {
	const topBarRef = useRef(null)
	const [markers, setMarkers] = useState({
		startMarker: null,
		endMarker: null,
	});
	const [prevMarkers, setPrevMarkers] = useState(null);
	const [rebuildPathState, setRebuildPathState] = useState(null);
	const [networkState, setNetworkState] = useState({
			pending: false,
			shortestDone: false,
  		secondDone: false,
  		error: null,
  		rebuildPath: false,
  });
  const [serviceArea, setServiceArea] = useState(null);
  const [actualShade, setActualShade] = useState(null);
  const [shade, setShade] = useState(null);
  const [shortestPath, setShortestPath] = useState([]);
  const [curTime, setCurTime] = React.useState(new Date());
  const [maxFactor, setMaxFactor] = React.useState(DEFAULT_FACTOR);
  const [preferShadow, setPreferShadow] = React.useState(true);
  const [secondPath, setSecondPath] = useState([]);
  const [sun, setSun] = useState(null);
  const [details, setDetails] = useState(null)
  const [sunStyle, setSunStyle] = useState({
    left: -50,
    top: -50,
    isUp: false,
  });
  const [winWidth, winHeight] = useWindowSize();
  const [map, setMap] = useState(null);
  const renderer = useMemo(() => new Canvas({ padding: 0.5, tolerance: 5 }), []);
  const debounceFetchPaths = useMemo(() => debounce(fetchPaths, 500), []);
  const debounceFetchShade = useMemo(() => debounce(fetchShade, 500), []);
  const debounceFetchSun = useMemo(() => debounce(fetchSun, 500), []);
  useEffect(() => {
    axios.get('/api/service-area').then((res) => {
        const area = [];
        res.data.polygon.forEach((point) => {
            area.push([point.lat, point.lon]);
        })
        setServiceArea(area);
    }).catch((e) => {
        toast.error('Failed to get service area!');
    });
  }, []);
  useEffect(() => {
    if (!sun || !winWidth || !winHeight) {
      return;
    }
    if (sun.elevation < 1) {
      setSunStyle({
        left: winWidth - SUN_MARGIN - SUN_SIZE,
        top: winHeight - SUN_MARGIN - SUN_SIZE,
        isUp: false,
      });
      return;
    }
    const diagonal = Math.sqrt(Math.pow(winWidth / 2, 2) + Math.pow(winHeight / 2, 2));
    let x = Math.sin(sun.azimuth) * diagonal;
    let y = -Math.cos(sun.azimuth) * diagonal;
    if (x > winWidth / 2 - SUN_MARGIN - SUN_SIZE) {
      const factor = (winWidth / 2 - SUN_MARGIN - SUN_SIZE) / x;
      x *= factor;
      y *= factor;
    } else if (x < -winWidth / 2 + SUN_MARGIN) {
      const factor = (-winWidth / 2 + SUN_MARGIN) / x;
      x *= factor;
      y *= factor;
    }
    if (y > winHeight / 2 - SUN_MARGIN - SUN_SIZE) {
      const factor = (winHeight / 2 - SUN_MARGIN - SUN_SIZE) / y;
      x *= factor;
      y *= factor;
    } else if (y < -winHeight / 2 + SUN_MARGIN) {
       const factor = (-winHeight / 2 + SUN_MARGIN) / y;
       x *= factor;
       y *= factor;
    }
    x += winWidth / 2;
    y += winHeight / 2;
    console.log(sun.azimuth, x, y, winWidth, winHeight);
    setSunStyle({
      left: x,
      top: y,
      isUp: true,
    });
  }, [sun, winWidth, winHeight]);
  useEffect(() => {
    if (!shortestPath || !secondPath || shortestPath.length === 0 || secondPath.length === 0) {
      setDetails(null);
      return;
    }
    const chartData = [[shortestPath, 'shortest', '#ff002b'], [secondPath, 'second', '#fb8500']].map(([arr, label, color]) => {
      const curDate = new Date(curTime);
      const timestamp = Date.UTC(
        curDate.getFullYear(),
        curDate.getMonth(),
        curDate.getDate(),
        curDate.getHours(),
        curDate.getMinutes(),
        curDate.getSeconds(),
        curDate.getMilliseconds(),
      );
      let totalWalkTime = new Date(timestamp).getTime();
      let totalSunCost = 0;
      return {
        label,
        color,
        data: arr.map((edge) => {
          const edgeCost = totalSunCost;
          const edgeTime = totalWalkTime;
          totalWalkTime += +edge.time;
          totalSunCost += (+edge.factor) * (+edge.distance);
          return {
            sunCost: edgeCost,
            time: new Date(edgeTime),
          }
        })
      };
    });
    const statistics = [[shortestPath, 'shortest'], [secondPath, 'second']].map(([path, label]) => {
      return {
        label,
        totalDistance: path.reduce((acc, edge) => acc + edge.distance, 0),
        totalTime: path.reduce((acc, edge) => acc + edge.time, 0),
        sunlightDistance: path.reduce((acc, edge) => {
          if (edge.factor >= 9) {
            return acc + edge.distance;
          }
          return acc;
        }, 0),
        sunlightTime: path.reduce((acc, edge) => {
          if (edge.factor >= 9) {
            return acc + edge.time;
          }
          return acc;
        }, 0),
      }
    });
    console.log(chartData, statistics);
    setDetails({
      chartData,
      statistics,
    });
  }, [shortestPath, secondPath])
  useEffect(() => {
      if (!networkState.rebuildPath) {
          return;
      }
      fetchPaths(markers, setMarkers, prevMarkers, setPrevMarkers, setNetworkState, setShortestPath, setSecondPath, curTime, maxFactor, preferShadow);
  }, [networkState.rebuildPath]);
  useEffect(() => {
      if (!prevMarkers) {
          return
      }
      debounceFetchPaths(markers, setMarkers, prevMarkers, setPrevMarkers, setNetworkState, setShortestPath, setSecondPath, curTime, maxFactor, preferShadow);
  }, [curTime, maxFactor, preferShadow]);
  useEffect(() => {
      if (!map) {
        return
      }
      debounceFetchShade(map, curTime, setShade);
  }, [map, curTime]);
  useEffect(() => {
      if (!map) {
        return;
      }
      debounceFetchSun(map, curTime, setSun);
  }, [map, curTime]);
  useEffect(() => {
      if (!networkState.pending) {
          return
      }
      if (networkState.shortestDone && networkState.secondDone) {
          topBarRef.current.complete()
      } else if (!networkState.shortestDone && !networkState.secondDone) {
          topBarRef.current.continuousStart()
      }
  }, [networkState.pending, networkState.shortestDone, networkState.secondDone]);
  useEffect(() => {
      if (!networkState.error) {
          return;
      }
      toast.error(networkState.error);
  }, [networkState.error]);
  useEffect(() => {
    if (!shade || shade.length === 0) {
      setActualShade([]);
      return
    }
    let shadeArray = [];
    let shadeChunks = shade;
    let shadeChunkIndex = 0;
    updateShadeChunk(shadeArray, shadeChunks, shadeChunkIndex, setActualShade);
  }, [shade]);
  const shadeOpacity = (shortestPath.length === 0 && secondPath.length === 0) ? 1 : 0.45;
  const sunColor = sunStyle.isUp ? "#fb8500" : "#023047";
  return (
  	<div className="App">
  		<LoadingBar
  		  color='#f11946'
  		  height={5}
  		  ref={topBarRef} />
  		<ToastContainer />
  		<BrowserView>
        <Tooltip placement="top" title={
          <div>
            { sun && sunStyle.isUp && `Azimuth is ${Math.round(sun.azimuth * 180 / Math.PI)}°`}
            { sun && sunStyle.isUp && <br /> }
            { sun && sunStyle.isUp && `Elevation is ${Math.round(sun.elevation)}°`}
            { !sunStyle.isUp && 'Sun is down'}
          </div>
        }>
          <div className="sun rotating" data-tip="sun-tooltip" style={{left: sunStyle.left, top: sunStyle.top}}>
            <svg width={SUN_SIZE} height={SUN_SIZE} viewBox="0 0 24 24" fill={sunColor}><path d="M3.55,18.54L4.96,19.95L6.76,18.16L5.34,16.74M11,22.45C11.32,22.45 13,22.45 13,22.45V19.5H11M12,5.5A6,6 0 0,0 6,11.5A6,6 0 0,0 12,17.5A6,6 0 0,0 18,11.5C18,8.18 15.31,5.5 12,5.5M20,12.5H23V10.5H20M17.24,18.16L19.04,19.95L20.45,18.54L18.66,16.74M20.45,4.46L19.04,3.05L17.24,4.84L18.66,6.26M13,0.55H11V3.5H13M4,10.5H1V12.5H4M6.76,4.84L4.96,3.05L3.55,4.46L5.34,6.26L6.76,4.84Z" /></svg>
          </div>
        </Tooltip>
      </BrowserView>
  		<div className="settings">
  			<Box
						sx={{
							position: 'relative',
							width: 280,
							height: 150,
							padding: 1.5,
							borderRadius: 3,
							backgroundColor: 'rgba(255,255,255,0.85)',
						}}>
						<LocalizationProvider dateAdapter={AdapterMoment}>
              <DateTimePicker
                renderInput={(props) => <TextField {...props} />}
                label="Departure time"
                value={curTime}
                onChange={(newValue) => {
                  setCurTime(newValue);
                }}
              />
            </LocalizationProvider>
            <Typography gutterBottom>
							Lightning factor
						</Typography>
						<Grid container spacing={1} alignItems="center">
							<Grid item>
								<Slider
								sx={{width: 245}}
									value={typeof maxFactor === 'number' ? maxFactor : 0}
									onChange={(_, newValue) => setMaxFactor(newValue)}
									defaultValue={DEFAULT_FACTOR}
									step={0.005}
									min={0}
									max={1} />
              </Grid>
						</Grid>
						<Grid container spacing={1} alignItems="center">
							<Grid item>
								<Switch
										checked={preferShadow}
										onChange={(e) => setPreferShadow(e.target.checked)}
										inputProps={{ 'aria-label': 'controlled' }}
									/>
							</Grid>
							<Grid item>
								{preferShadow ? 'Shady way' : 'Sunny way'}
							</Grid>
						</Grid>
				</Box>
  		</div>
  		<BrowserView>
  		  {
  		    details && <div className="details">
            <Box
              sx={{
              position: 'relative',
              width: 500,
              height: 250,
              padding: 1.5,
              borderRadius: 3,
              backgroundColor: 'rgba(255,255,255,0.85)',
            }}>
              {
                details?.chartData && <MemoChartComponent chartData={details.chartData} />
              }
              {
                details?.statistics && <MemoStatisticsComponent data={details.statistics} />
              }
            </Box>
          </div>
  		  }
  		</BrowserView>
			<MapContainer
				className="map"
				center={[41.3874, 2.1686]}
				minZoom={11} maxZoom={18}
				renderer={renderer}
				zoom={15}
				preferCanvas={true}
				whenCreated={setMap}>
				<TileLayer
					url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
				/>
				<Pane name="serviceArea" style={{ zIndex: 410 }}>
				  { serviceArea && <Polyline pathOptions={{color: 'red', weight: 5}} positions={serviceArea} /> }
				</Pane>
				<Pane name="shortestPath" style={{ zIndex: 403 }}>
          { shortestPath && <MemoPathComponent path={shortestPath} defaultColor={true} />}
        </Pane>
        <Pane name="secondPath" style={{ zIndex: 404 }}>
          { secondPath && <MemoPathComponent path={secondPath} defaultColor={false} />}
        </Pane>
        <Pane name="markers" style={{ zIndex: 408 }}>
          { markers.startMarker && <Marker position={markers.startMarker} /> }
          { markers.endMarker && <Marker position={markers.endMarker} /> }
        </Pane>
				<Pane name="shade" style={{ zIndex: 401 }}>
				  { actualShade && actualShade.map((actualShadeShard) => (
				    <MemoShadeComponent path={actualShadeShard} opacity={shadeOpacity}/>
				  ))}
				</Pane>
				<MapListener
					markers={markers}
					setMarkers={setMarkers}
					networkState={networkState}
					setNetworkState={setNetworkState}
					setShortestPath={setShortestPath}
					setSecondPath={setSecondPath}
					setShade={setShade}
					setSun={setSun}
					curTime={curTime} />
			</MapContainer>
  	</div>
  );
}

export default App;
