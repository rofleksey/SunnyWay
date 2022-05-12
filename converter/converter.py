import csv
import gc
import itertools
import math
import utm
import warnings
from dataclasses import dataclass
from enum import Enum
from geographiclib.geodesic import Geodesic
from lxml import etree
from shapely.errors import ShapelyDeprecationWarning
from shapely.geometry import LineString
from shapely.geometry import Point as ShapelyPoint
from shapely.geometry.collection import GeometryCollection
from shapely.geometry.multipoint import MultiPoint
from shapely.geometry.multipolygon import MultiPolygon
from shapely.geometry.polygon import Polygon
from shapely.validation import make_valid
from sklearn.neighbors import BallTree
from tqdm import tqdm
from typing import Any, List

warnings.filterwarnings("ignore", category=ShapelyDeprecationWarning)

TREE_RADIUS = 0.5
SCAN_RADIUS = 25
EARTH_RADIUS = 6371000
MAX_BUILDING_NODE_COUNT = 50000
MAX_EDGE_SIZE = 10000

class WayType(Enum):
    NONE = 0
    ROAD = 1
    BUILDING = 2


@dataclass
class Node:
    lat: float
    lon: float
    shapely_point: ShapelyPoint
    parent: Any = None

    def to_point(self):
        return ShapelyPoint(lat, lon)


@dataclass(frozen=True)
class Road:
    nodes: List[Node]
    avoid: bool


@dataclass(frozen=True)
class BuildingScanResult:
    left: float
    right: float


@dataclass(frozen=True)
class Building:
    id: int
    shape: Any

    def __hash__(self):
        return hash(id)

    def __eq__(self, other):
        return self.id == other.id


@dataclass(frozen=True)
class Tree:
    id: int
    shape: Any

    def __hash__(self):
        return hash(id)

    def __eq__(self, other):
        return self.id == other.id


@dataclass(frozen=True)
class Point:
    lat: float
    lon: float


# @dataclass(frozen=True)
# class Rectangle:
#     points: List[Point]

#     def center(self):
#         lon_sum = 0
#         lat_sum = 0
#         for point in self.points:
#             lon_sum += point.lon
#             lat_sum += point.lat
#         return Point(lat_sum / len(self.points), lon_sum / len(self.points))


@dataclass(frozen=True)
class Edge:
    start: Point
    end: Point
    left_shadow: float
    right_shadow: float
    distance: float
    direction: float
    avoid: bool

    def to_dict(self):
        return {'start_lat': self.start.lat, 'start_lon': self.start.lon, 'end_lat': self.end.lat,
                'end_lon': self.end.lon, 'left_shadow': self.left_shadow, 'right_shadow': self.right_shadow,
                'distance': self.distance, 'direction': self.direction, 'avoid': self.avoid}

def consecutive_item_pairs(iterable):
    iters = itertools.tee(iterable, 2)
    for i, it in enumerate(iters):
        next(itertools.islice(it, i, i), None)
    return zip(*iters)


print('Loading map data...')
with open('map.osm', mode="r", encoding="utf-8") as map_file:
    tree = etree.parse(map_file)
    map = tree.getroot()
print('Parsing xml objects...')
external_node_list = map.findall('.//node')
external_way_list = map.findall('.//way')

print('Doing garbage collection...')
del map
del tree
gc.collect()

node_map = {}
building_node_list = []
tree_node_list = []
road_list = []
buildings_list = []

print('Loading nodes...')
for node in tqdm(external_node_list):
    tags = node.findall('.//tag')
    is_tree = False
    for tag in tags:
        key = tag.get('k')
        value = tag.get('v')
        if key == 'natural' and value == 'tree':
            is_tree = True
    id = int(node.get('id'))
    lat = float(node.get('lat'))
    lon = float(node.get('lon'))
    x, y, _, _ = utm.from_latlon(lat, lon)
    shapely_point = ShapelyPoint(x, y)
    new_node = Node(lat, lon, shapely_point)
    if is_tree:
        new_tree = Tree(id, shapely_point)
        new_node.parent = new_tree
        tree_node_list.append(new_node)
    node_map[id] = new_node

highway_types = set()

print('Loading roads and buildings...')
for way in tqdm(external_way_list):
    tags = way.findall('.//tag')
    node_tags = way.findall('.//nd')
    way_type = WayType.NONE
    id = way.get('id')
    avoid = False
    details = ""
    for tag in tags:
        key = tag.get('k')
        value = tag.get('v')
        if key == 'highway':
            if value == 'residential' or value == 'unclassified' or value == 'platform' or value == 'corridor' or value == 'elevator' or value == 'track' or value == 'cycleway' or value == 'living_street' or value == 'footway' or value == 'bridleway' or value == 'service' or value == 'pedestrian' or value == 'steps' or value == 'path':
                if value == 'residential':
                    avoid = True
                way_type = WayType.ROAD
            elif not (value in highway_types):
                print(f'new highway type: {value}')
                highway_types.add(value)
        elif key == 'natural':
            if value == 'tree_row':
                way_type = WayType.BUILDING
        elif key == 'barrier':
            if value == 'hedge' or value == 'wall' or value == 'city_wall' or value == 'wall' or value == 'retaining_wall':
                way_type = WayType.BUILDING
        elif 'building' in key:
            way_type = WayType.BUILDING

        if key == 'layer':
            if float(value) < -1:
                way_type = WayType.NONE
    if way_type == WayType.NONE:
        continue
    node_list = []
    for node_tag in node_tags:
        ref = int(node_tag.get('ref'))
        target_node = node_map[ref]
        if not target_node:
            raise Exception(f"Can't find node #{ref}")
        node_list.append(target_node)
    if way_type == WayType.BUILDING:
        if len(node_list) > 2:
            shape = Polygon([(n.shapely_point.x, n.shapely_point.y) for n in node_list])
        elif len(node_list) == 2:
            shape = LineString([(n.shapely_point.x, n.shapely_point.y) for n in node_list])
        elif len(node_list) == 1:
            shape = ShapelyPoint(node_list[0].shapely_point.x, node_list[0].shapely_point.y)
        else:
            shape = LineString()
        new_building = Building(id, shape)
        for node in node_list:
            building_node_list.append(node)
            node.parent = new_building
        buildings_list.append(new_building)
    elif way_type == WayType.ROAD:
        # print(f'result = {temp_name}')
        new_road = Road(node_list, avoid)
        road_list.append(new_road)

print("Doing garbage collection...")

del external_node_list
del external_way_list
gc.collect()

print(
    f'Loaded {len(node_map)} nodes, {len(road_list)} roads, {len(tree_node_list)} trees and {len(buildings_list)} buildings')

print('Building trees...')

building_tree = BallTree([[node.shapely_point.x, node.shapely_point.y] for node in building_node_list])
tree_tree = BallTree([[node.shapely_point.x, node.shapely_point.y] for node in tree_node_list])


def break_geometry(geom, list, depthLimiter):
    if depthLimiter == 0:
        return
    if isinstance(geom, ShapelyPoint):
        return
    if isinstance(geom, GeometryCollection) or isinstance(geom, MultiPoint):
        for p in geom.geoms:
            break_geometry(p, list, depthLimiter - 1)
            return
    if isinstance(geom, MultiPolygon):
        for p in geom.geoms:
            list.append(p)
            return
    list.append(geom)


def get_buildings_in_rect(polygon, distance):
    center = polygon.centroid
    close_indices = building_tree.query_radius(
        [[center.x, center.y]], distance)
    parent_set = set()
    # prevents scanning edge with 896824 buildings on it
    if len(close_indices[0]) > MAX_BUILDING_NODE_COUNT:
        print(f'Ignored polygon with {len(close_indices[0])} building nodes near it (distance = {distance})')
        return []
    for i in close_indices[0]:
        node = building_node_list[i]
        parent_set.add(node.parent)
    result = []
    for parent in parent_set:
        try:
            if parent.shape.intersects(polygon):
                inter = make_valid(parent.shape).intersection(polygon)
                break_geometry(inter, result, 10)
        except BaseException as e:
            print(f'Error intersecting forms (building): {e}')
    return result


def get_trees_in_rect(polygon, distance):
    center = polygon.centroid
    close_indices = tree_tree.query_radius(
        [[center.x, center.y]], distance)
    parent_set = set()
    # prevents scanning edge with 896824 buildings on it
    if len(close_indices[0]) > MAX_BUILDING_NODE_COUNT:
        print(f'Ignored polygon with {len(close_indices[0])} building nodes near it (distance = {distance})')
        return []
    for i in close_indices[0]:
        node = tree_node_list[i]
        parent_set.add(node.parent)
    result = []
    for parent in parent_set:
        try:
            if polygon.contains(parent.shape):
                result.append(parent.shape)
        except BaseException as e:
            print(f'Error intersecting forms (tree): {e}')
    return result


def calculate_shadow_size(buildings, trees, segment):
    if segment.length == 0:
        return
    segments = []
    for building in buildings:
        min_dist = None
        max_dist = None
        if isinstance(building, LineString):
            for x, y in building.coords:
                exterior_point = ShapelyPoint(x, y)
                proj_dist = segment.project(exterior_point, normalized=True)
                if min_dist is None or min_dist > proj_dist:
                    min_dist = proj_dist
                if max_dist is None or max_dist < proj_dist:
                    max_dist = proj_dist
            if min_dist != max_dist:
                segments.append((min_dist, max_dist))
        else:
            for x, y in building.exterior.coords:
                exterior_point = ShapelyPoint(x, y)
                proj_dist = segment.project(exterior_point, normalized=True)
                if min_dist is None or min_dist > proj_dist:
                    min_dist = proj_dist
                if max_dist is None or max_dist < proj_dist:
                    max_dist = proj_dist
            if min_dist != max_dist:
                segments.append((min_dist, max_dist))
    for point in trees:
        proj_dist = segment.project(point, normalized=False)
        min_dist = max(0, proj_dist - TREE_RADIUS / 2) / segment.length
        max_dist = min(segment.length, proj_dist + TREE_RADIUS / 2) / segment.length
        if min_dist != max_dist:
            segments.append((min_dist, max_dist))
    if len(segments) == 0:
        return 0
    # Klee’s Algorithm
    points = [0] * (len(segments) * 2)
    for i, s in enumerate(segments):
        points[i * 2] = (s[0], False)
        points[i * 2 + 1] = (s[1], True)
    points.sort(key=lambda x: x[0])
    counter = 0
    result = 0
    for i in range(len(points)):
        if counter > 0:
            result += points[i][0] - points[i-1][0]
        if points[i][1]:
            counter -= 1
        else:
            counter += 1
    return result


def scan_buildings(n1, n2):
    segment = LineString([n1.shapely_point, n2.shapely_point])
    segment_reverse = LineString([n2.shapely_point, n1.shapely_point])
    shapely_n2_left = segment.parallel_offset(SCAN_RADIUS, 'left').boundary[1]
    shapely_n2_right = segment.parallel_offset(SCAN_RADIUS, 'right').boundary[0]
    shapely_n1_right = segment_reverse.parallel_offset(SCAN_RADIUS, 'left').boundary[1]
    shapely_n1_left = segment_reverse.parallel_offset(SCAN_RADIUS, 'right').boundary[0]
    left_rect = Polygon([n1.shapely_point, shapely_n1_left,
                         shapely_n2_left, n2.shapely_point])
    right_rect = Polygon([n1.shapely_point, shapely_n1_right,
                          shapely_n2_right, n2.shapely_point])
    distance = 2 * math.sqrt(segment.length ** 2 + SCAN_RADIUS ** 2)
    left_buildings = get_buildings_in_rect(left_rect, distance)
    right_buildings = get_buildings_in_rect(right_rect, distance)
    left_trees = get_trees_in_rect(left_rect, distance)
    right_trees = get_trees_in_rect(right_rect, distance)
    left_shadow = calculate_shadow_size(left_buildings, left_trees, segment)
    right_shadow = calculate_shadow_size(right_buildings, right_trees, segment)
    return BuildingScanResult(left=left_shadow, right=right_shadow)


print('Writing edges...')
with open('output.csv', mode='w', encoding="utf-8") as csv_file:
    fieldnames = ['start_lat', 'start_lon', 'end_lat', 'end_lon',
                  'left_shadow', 'right_shadow', 'distance', 'direction', 'avoid']
    writer = csv.DictWriter(csv_file, fieldnames=fieldnames, dialect="unix")
    writer.writeheader()
    #[138560:]
    for road in tqdm(road_list):
        for n1, n2 in consecutive_item_pairs(road.nodes):
            geo_result = Geodesic.WGS84.Inverse(n1.lat, n1.lon, n2.lat, n2.lon)
            line_length = geo_result['s12']
            if line_length > MAX_EDGE_SIZE:
                print(f'Ignored edge with length {line_length}')
                continue
            direction = geo_result['azi1']
            scan_result = scan_buildings(n1, n2)
            new_edge = Edge(n1, n2, scan_result.left, scan_result.right, line_length, direction, road.avoid)
            writer.writerow(new_edge.to_dict())
print("Finishing...")
