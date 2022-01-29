import './App.css';
import { isDev } from './lib'
import axios from 'axios';
import { Canvas } from 'leaflet';
import { MapContainer, MapConsumer, TileLayer, Marker, Polyline, Popup, useMapEvent } from 'react-leaflet';
import { debounce } from 'lodash';
import React, { useState, useEffect, useMemo, useRef } from 'react';
import LoadingBar from 'react-top-loading-bar'

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

import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

function postPath(markers, curTime, timeSampling, preferShadow, algorithm, property, setPath, setNetworkState) {
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
		timeSampling,
		preferShadow,
		algorithm,
	}).then((res) => {
		const array = [];
		res.data.result.path.forEach((edge) => {
			const {fromPoint, toPoint, edgeId} = edge;
			let content = [];
			content.push(`distance = ${edge.distance.toFixed(2)}`);
			content.push(`time to walk (minutes) = ${(edge.time / 60000).toFixed(2)}`);
			if (edge.cost > 0) {
				content.push(`cost = ${edge.cost.toFixed(2)}`);
			}
			Object.entries(edge.metadata).forEach(([key, value]) => {
				if (typeof value === 'number') {
					value = +value.toFixed(2);
				}
				content.push(`${key} = ${value}`);
			});
			array.push({
				id: edgeId,
				positions: [[fromPoint.lat, fromPoint.lon], [toPoint.lat, toPoint.lon]],
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
		const errStr = e?.response?.data?.message ?? e.toString();
		setNetworkState((oldState) => ({
			...oldState,
			error: errStr,
			shortestDone: true,
			secondDone: true,
		}));
	});
}

function MapListener(props) {
	const {markers, setMarkers, networkState, setNetworkState, setShortestPath, setSecondPath} = props;
	const map = useMapEvent('click', (e) => {
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
	});
	return null;
}

function fetchPaths(markers, setMarkers, prevMarkers, setPrevMarkers, setNetworkState, setShortestPath, setSecondPath, curTime, timeSampling, preferShadow) {
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
	postPath(curMarkers, restTime, timeSampling * 60000, preferShadow, 'distance', 'shortestDone', setShortestPath, setNetworkState);
	postPath(curMarkers, restTime, timeSampling * 60000, preferShadow, 'shadow', 'secondDone', setSecondPath, setNetworkState);
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

function PathComponent(props) {
	const { color, path } = props;
	return path.map(({id, positions, content}) => (
		<Polyline
			key={id}
			onMouseOver={(e) => {
				e.target.openPopup();
			}}
			onMouseOut={(e) => {
				e.target.closePopup();
			}}
			pathOptions={{color, weight: 2}}
			positions={positions}>
			<Popup>
				<p>
				{content.map((s) => ([s, <br key="br"/>]))}
				</p>
			</Popup>
		</Polyline>
	))
}

const MemoPathComponent = React.memo(PathComponent);

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
	const [shortestPath, setShortestPath] = useState([]);
	const [curTime, setCurTime] = React.useState(new Date());
	const [timeSampling, setTimeSampling] = React.useState(15);
	const [preferShadow, setPreferShadow] = React.useState(true);
	const [secondPath, setSecondPath] = useState([]);
	const renderer = useMemo(() => new Canvas({ padding: 0.5, tolerance: 5 }), []);
	const debounceFetchPaths = useMemo(() => debounce(fetchPaths, 500), []);
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
		if (!networkState.rebuildPath) {
			return;
		}
		fetchPaths(markers, setMarkers, prevMarkers, setPrevMarkers, setNetworkState, setShortestPath, setSecondPath, curTime, timeSampling, preferShadow);
	}, [networkState.rebuildPath]);
	useEffect(() => {
		if (!prevMarkers) {
			return
		}
		debounceFetchPaths(markers, setMarkers, prevMarkers, setPrevMarkers, setNetworkState, setShortestPath, setSecondPath, curTime, timeSampling, preferShadow);
	}, [curTime, timeSampling, preferShadow]);
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
	}, [networkState.error])
  return (
  	<div className="App">
  		<LoadingBar color='#f11946' ref={topBarRef} />
  		<ToastContainer />
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
                label="Время выхода"
                value={curTime}
                onChange={(newValue) => {
                  setCurTime(newValue);
                }}
              />
            </LocalizationProvider>
            <Typography gutterBottom>
							Дискретизация времени (минуты)
						</Typography>
						<Grid container spacing={1} alignItems="center">
							<Grid item>
								<Slider
								sx={{width: 245}}
									value={typeof timeSampling === 'number' ? timeSampling : 0}
									onChange={(_, newValue) => setTimeSampling(newValue)}
									defaultValue={15}
									step={5}
									min={5}
									max={120} />
              </Grid>
              <Grid item>
								{timeSampling}
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
								{preferShadow ? 'Маршрут по тени' : 'Маршрут по солнцу'}
							</Grid>
						</Grid>
				</Box>
  		</div>
			<MapContainer
				className="map"
				center={[59.987325, 30.342377]}
				minZoom={11} maxZoom={18}
				renderer={renderer}
				zoom={18}
				preferCanvas={true}>
				<TileLayer
					url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
				/>
				{ serviceArea && <Polyline pathOptions={{color: 'red', weight: 5}} positions={serviceArea} /> }
				{ markers.startMarker && <Marker position={markers.startMarker} /> }
				{ markers.endMarker && <Marker position={markers.endMarker} /> }
				{ shortestPath && <MemoPathComponent color='green' path={shortestPath} />}
				{ secondPath && <MemoPathComponent color='red' path={secondPath} />}
				<MapListener
					markers={markers}
					setMarkers={setMarkers}
					networkState={networkState}
					setNetworkState={setNetworkState}
					setShortestPath={setShortestPath}
					setSecondPath={setSecondPath}/>
			</MapContainer>
  	</div>
  );
}

export default App;
