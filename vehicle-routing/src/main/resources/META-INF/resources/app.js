const colors = [
  'aqua',
  'aquamarine',
  'blue',
  'blueviolet',
  'chocolate',
  'cornflowerblue',
  'crimson',
  'forestgreen',
  'gold',
  'lawngreen',
  'limegreen',
  'maroon',
  'mediumvioletred',
  'orange',
  'slateblue',
  'tomato',
];
let autoRefreshCount = 0;
let autoRefreshIntervalId = null;

let initialized = false;
const depotByIdMap = new Map();
const customerByIdMap = new Map();

const solveButton = $('#solveButton');
const stopSolvingButton = $('#stopSolvingButton');
const vehiclesTable = $('#vehicles');
const depotsTable = $('#depots');

const colorById = (i) => colors[i % colors.length];
const colorByVehicle = (vehicle) => vehicle === null ? null : colorById(vehicle.id);
const colorByDepot = (depot) => depot === null ? null : colorById(depot.id);

const defaultIcon = new L.Icon.Default();
const greyIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-grey.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.6.0/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

const fetchHeaders = {
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
};

const formatDistance = (distanceInMeters) => `${Math.floor(distanceInMeters / 1000)}km ${distanceInMeters % 1000}m`;

const getStatus = () => {
  fetch('/vrp/status', fetchHeaders)
    .then((response) => {
      if (!response.ok) {
        return handleErrorResponse('Get status failed', response);
      } else {
        return response.json().then((data) => showProblem(data));
      }
    })
    .catch((error) => handleClientError('Failed to process response', error));
};

const solve = () => {
  fetch('/vrp/solve', { ...fetchHeaders, method: 'POST' })
    .then((response) => {
      if (!response.ok) {
        return handleErrorResponse('Start solving failed', response);
      } else {
        updateSolvingStatus(true);
        autoRefreshCount = 300;
        if (autoRefreshIntervalId == null) {
          autoRefreshIntervalId = setInterval(autoRefresh, 500);
        }
      }
    })
    .catch((error) => handleClientError('Failed to process response', error));
};

const stopSolving = () => {
  fetch('/vrp/stopSolving', { ...fetchHeaders, method: 'POST' })
    .then((response) => {
      if (!response.ok) {
        return handleErrorResponse('Stop solving failed', response);
      } else {
        updateSolvingStatus(false);
        getStatus();
      }
    })
    .catch((error) => handleClientError('Failed to process response', error));
};

const formatErrorResponseBody = (body) => {
  // JSON must not contain \t (Quarkus bug)
  const json = JSON.parse(body.replace(/\t/g, '  '));
  return `${json.details}\n${json.stack}`;
};

const handleErrorResponse = (title, response) => {
  return response.text()
    .then((body) => {
      const message = `${title} (${response.status}: ${response.statusText}).`;
      const stackTrace = body ? formatErrorResponseBody(body) : '';
      showError(message, stackTrace);
    });
};

const handleClientError = (title, error) => {
  console.error(error);
  showError(`${title}.`,
    // Stack looks differently in Chrome and Firefox.
    error.stack.startsWith(error.name)
      ? error.stack
      : `${error.name}: ${error.message}\n    ${error.stack.replace(/\n/g, '\n    ')}`);
};

const showError = (message, stackTrace) => {
  const notification = $(`<div class="toast shadow rounded-lg" role="alert" style="min-width: 30rem"/>`)
    .append($(
      `<div class="toast-header bg-danger">
<strong class="mr-auto text-dark">Error</strong>
<button type="button" class="ml-2 mb-1 close" data-dismiss="toast">
<span>&times;</span>
</button>
</div>`))
    .append($(`<div class="toast-body"/>`)
      .append($(`<p/>`).text(message))
      .append($(`<pre/>`)
        .append($(`<code/>`).text(stackTrace)),
      ),
    );
  $('#notificationPanel').append(notification);
  notification.toast({ autohide: false });
  notification.toast('show');
};

const updateSolvingStatus = (solving) => {
  if (solving) {
    solveButton.hide();
    stopSolvingButton.show();
    autoRefreshCount = 40;
  } else {
    solveButton.show();
    stopSolvingButton.hide();
  }
};

const autoRefresh = () => {
  getStatus();
  autoRefreshCount--;
  if (autoRefreshCount <= 0) {
    clearInterval(autoRefreshIntervalId);
    autoRefreshIntervalId = null;
  }
};

const depotPopupContent = (depot, color) => `<h5>Depot ${depot.id}</h5>
<ul class="list-unstyled">
<li><span style="background-color: ${color}; display: inline-block; width: 12px; height: 12px; text-align: center">
</span> ${color}</li>
</ul>`;

const customerPopupContent = (customer) => `<h5>Customer ${customer.id}</h5>
Demand: ${customer.demand}<br>
TimeWindow: ${customer.beginServiceWindow} - ${customer.endServiceWindow}<br>
ServiceTime: ${customer.serviceTime}`;

const getDepotMarker = ({ id, location }) => {
  let marker = depotByIdMap.get(id);
  if (marker) {
    return marker;
  }
  marker = L.marker(location);
  marker.addTo(depotGroup).bindPopup();
  depotByIdMap.set(id, marker);
  return marker;
};

const getCustomerMarker = ({ id, location }) => {
  let marker = customerByIdMap.get(id);
  if (marker) {
    return marker;
  }
  marker = L.circleMarker(location);
  marker.addTo(customerGroup).bindPopup();
  customerByIdMap.set(id, marker);
  return marker;
};

const showProblem = ({ solution, scoreExplanation, isSolving , idBestSolution, solutionIteration, currentIteration}) => {
  if (!initialized) {
    initialized = true;
    map.fitBounds(solution.bounds);
  }
  // Vehicles
  $('[data-toggle="tooltip-load"]').tooltip('dispose');
  vehiclesTable.children().remove();
  solution.usedVehicleList.forEach((vehicle) => {
    const { id, capacity, totalDemand, totalDistanceMeters, totalTime, noCustomers, customerIds} = vehicle;
    const percentage = totalDemand / capacity * 100;
    const color = colorByVehicle(vehicle);
    const colorIfUsed = color;
    vehiclesTable.append(`
      <tr>
        <td>
          <i class="fas fa-crosshairs" id="crosshairs-${id}"
            style="background-color: ${colorIfUsed}; display: inline-block; width: 1rem; height: 1rem; text-align: center">
          </i>
        </td>
        <td>Vehicle ${id}</td>
        <td>
          <div class="progress" data-toggle="tooltip-load" data-placement="left" data-html="true"
            title="Cargo: ${totalDemand}<br/>Capacity: ${capacity}">
            <div class="progress-bar" role="progressbar" style="width: ${percentage}%">${totalDemand}/${capacity}</div>
          </div>
        </td>
        <td>${noCustomers}: ${customerIds.toString()}</td>
        <td>${formatDistance(totalDistanceMeters)}</td>
        <td>${totalTime}</td>
        <td>
      </tr>`);
  });
  $('[data-toggle="tooltip-load"]').tooltip();
  // Depots
  depotsTable.children().remove();
  solution.depotList.forEach((depot) => {
    const { id } = depot;
    const vehicles = solution.vehicleList;
    let noVehicles = 0;
    vehicles.forEach((vehicle) => {
      if(vehicle.depot.id === id)
        noVehicles++;
    });
    const usedVehicles = solution.usedVehicleList;
    let noUsedVehicles = 0;
    usedVehicles.forEach((vehicle) => {
      if(vehicle.depot.id === id)
      noUsedVehicles++;
    });
    const color = colorByDepot(depot);
    const icon = defaultIcon;
    const marker = getDepotMarker(depot);
    marker.setIcon(icon);
    marker.setPopupContent(depotPopupContent(depot, color));
    depotsTable.append(`<tr>
      <td><i class="fas fa-crosshairs" id="crosshairs-${id}"
      style="background-color: ${color}; display: inline-block; width: 1rem; height: 1rem; text-align: center">
      </i></td><td>Depot ${id}: ${noUsedVehicles}/${noVehicles} vehicles in use</td>
      </tr>`);
  });
  // Customers
  solution.customerList.forEach((customer) => {
    getCustomerMarker(customer).setPopupContent(customerPopupContent(customer));
  });
  // Route
  routeGroup.clearLayers();
  solution.usedVehicleList.forEach((vehicle) => {
    L.polyline(vehicle.route, { color: colorByVehicle(vehicle) }).addTo(routeGroup);
  });

  // Summary
  $('#solutionId').text(idBestSolution);
  $('#solutionIteration').text(solutionIteration);
  $('#currentIteration').text(currentIteration);
  $('#score').text(solution.score);
  $('#scoreInfo').text(scoreExplanation);
  $('#distance').text(formatDistance(solution.distanceMeters));
  $('#time').text(solution.totalTime)
  $('#fixCost').text(solution.fixCost)
  updateSolvingStatus(isSolving);
};

const map = L.map('map', { doubleClickZoom: false }).setView([51.505, -0.09], 13);
map.whenReady(getStatus);

L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
  maxZoom: 19,
  attribution: '&copy; <a href="https://www.openstreetmap.org/">OpenStreetMap</a> contributors',
}).addTo(map);

const customerGroup = L.layerGroup().addTo(map);
const depotGroup = L.layerGroup().addTo(map);
const routeGroup = L.layerGroup().addTo(map);

solveButton.click(solve);
stopSolvingButton.click(stopSolving);

updateSolvingStatus();
$('[data-toggle="tooltip"]').tooltip();
