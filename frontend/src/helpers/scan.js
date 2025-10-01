import { model, ErrorStatus } from "@/model.js";
import { setCbom } from "@/helpers.js";
import uuid4 from "uuid4";
import { API_SCAN_URL } from "@/app.config";

export const STATES = {
  LOADED: 'loaded',
  ERROR: 'error',
  LOADING: 'loading',
  ENDING: 'ending',
};

// This var is set to true when the client closes the socket. It is reinitialized to false when a new socket is created.
var socketWasManuallyClosed = false;

function startWebSocket(socketURL) {
  // Check if WebSocket is already open
  if (
    model.scanning.socket &&
    model.scanning.socket.readyState === WebSocket.OPEN
  ) {
    console.error("WebSocket is already open.");
    model.addError(null); // Display 'Unknown error'
    return;
  }

  // Create a new WebSocket instance
  model.scanning.socket = new WebSocket(socketURL);
  // Reset manual closing boolean tracker
  socketWasManuallyClosed = false;

  // WebSocket event listeners
  model.scanning.socket.addEventListener("open", () => {
    console.log("WebSocket connection opened.");
    scan();
  });

  model.scanning.socket.addEventListener("message", (event) => {
    // console.log('Received message:', event.data);
    let message = String(event.data.trim());
    handleMessage(message);
  });

  model.scanning.socket.addEventListener("close", (event) => {
    console.log("WebSocket connection closed.", event);
  });

  model.scanning.socket.addEventListener("error", (error) => {
    if (socketWasManuallyClosed) {
      // In safari, manually closing the connection creates an error:
      // Do not display an error when the connection is manually closed by the user
      console.warn(
        "The connection was closed by the client. An connection error occurred, but has NOT been notified in the UI."
      );
    } else {
      console.error("WebSocket error:", error);
      model.addError(ErrorStatus.NoConnection);
    }
  });
}

export function stopWebSocket() {
  // Check if WebSocket is not null and is open
  if (
    model.scanning.socket &&
    model.scanning.socket.readyState === WebSocket.OPEN
  ) {
    socketWasManuallyClosed = true;
    model.scanning.socket.close(); // Close the WebSocket connection
    console.log("The client asked to close the WebSocket");
  }
  // else {
  //     console.warn('WebSocket is not open.');
  // }
}

export function connectAndScan(gitBranch, gitSubfolder, credentials) {
  model.resetScanningInfo();
  setCodeOrigin(gitBranch, gitSubfolder);
  setCredentials(credentials)
  let clientId = uuid4();
  let socketURL = `${API_SCAN_URL}/${clientId}`;
  startWebSocket(socketURL);
}

function scan() {
  if (!model.scanning.socket) {
    model.addError(ErrorStatus.NoConnection);
    console.log("No socket in model");
  } else if (!model.codeOrigin.scanUrl) {
    model.addError(ErrorStatus.InvalidRepo);
    console.log("Not valid Git URL or Package URL");
  } else {
    // build scan request
    const scanRequest = {};
    // set scan options
    scanRequest["scanUrl"] = model.codeOrigin.scanUrl;
    if (model.codeOrigin.revision) {
      scanRequest["branch"] = model.codeOrigin.revision;
    }
    if (model.codeOrigin.subfolder) {
      scanRequest["subfolder"] = model.codeOrigin.subfolder;
    }
    // set credentials
    if (model.credentials.pat) {
      scanRequest["credentials"] = {}
      scanRequest["credentials"]["pat"] = model.credentials.pat;
    } else if (model.credentials.username && model.credentials.password) {
      scanRequest["credentials"] = {}
      scanRequest["credentials"]["username"] = model.credentials.username;
      scanRequest["credentials"]["password"] = model.credentials.password;
    }

    model.scanning.socket.send(JSON.stringify(scanRequest));
    // this.filterOpen = false
    model.scanning.isScanning = true;
    model.scanning.scanningStatus = STATES.LOADING;
    model.showResults = true;
  }
}

function handleMessage(messageJson) {
  let obj = JSON.parse(messageJson);
  // console.log(obj)
  if (obj["type"] === "LABEL") {
    model.scanning.scanningStatusMessage = obj["message"];
    if (obj["message"] === "Starting...") {
      model.scanning.startTime = new Date();
    }
    if (obj["message"] === "Finished") {
      model.scanning.scanningStatus = STATES.LOADED;
      model.scanning.isScanning = false;
      let finishTime = new Date();
      model.scanning.totalDuration = Math.floor(
        (finishTime - model.scanning.startTime) / 1000
      ); // Time in seconds
    }
  } else if (obj["type"] === "ERROR") {
    model.addError(ErrorStatus.ScanError, model.scanning.scanningStatusMessage = obj["message"]); //
    // update state
    model.scanning.scanningStatusMessage = obj["message"];
    model.scanning.scanningStatus = STATES.ERROR;
    model.scanning.isScanning = false;
    // log
    console.error("Error:", model.scanning.scanningStatusMessage);
  } else if (obj["type"] === "WARNING") {
    model.addError(ErrorStatus.ScanWarning, model.scanning.scanningStatusMessage = obj["message"]); //
    // log
    console.warn("Warning:", model.scanning.scanningStatusMessage);
  } else if (obj["type"] === "DETECTION") {
    let cryptoAssetJson = obj["message"];
    const cryptoAsset = JSON.parse(cryptoAssetJson);
    model.scanning.liveDetections.push(cryptoAsset);
    // console.log("New detection:",obj)
  } else if (obj["type"] === "CBOM") {
    let cbomString = obj["message"];
    setCbom(JSON.parse(cbomString));
    console.log("Received CBOM from scanning:", model.cbom);
  } else if (obj["type"] === "GITURL") {
    model.codeOrigin.gitUrl = obj["message"];
  } else if (obj["type"] === "BRANCH") {
    model.codeOrigin.revision = obj["message"];
  } else if (obj["type"] === "FOLDER") {
    model.codeOrigin.subfolder = obj["message"];
  } else if (obj["type"] === "SCANNED_FILE_COUNT") {
    model.scanning.numberOfFiles = obj["message"];
  } else if (obj["type"] === "SCANNED_NUMBER_OF_LINES") {
    model.scanning.numberOfLines = obj["message"];
  } else if (obj["type"] === "SCANNED_DURATION") {
    model.scanning.scanDuration = obj["message"];
  } else if (obj["type"] === "REVISION_HASH") {
    model.codeOrigin.commitID = obj["message"];
  } else {
    console.log("Unknown message:", messageJson);
  }
}

function setCodeOrigin(gitBranch, gitSubfolder) {
  if (model.codeOrigin.scanUrl) {
    model.codeOrigin.scanUrl = model.codeOrigin.scanUrl.trim();
    // if it's not a package url
    if (!model.codeOrigin.scanUrl.startsWith("pkg:")) {
      // normalize scanUrl
      model.codeOrigin.scanUrl = model.codeOrigin.scanUrl
          .replace(/^scm:git:git:\/\//, "")
          .replace(/.git$/, "");
      if (!model.codeOrigin.scanUrl.contain("://")) {
        model.codeOrigin.scanUrl = "https://" + model.codeOrigin.scanUrl;
      }
    }
  }
  if (gitBranch) {
    model.codeOrigin.revision = gitBranch.trim();
  }
  if (gitSubfolder) {
    model.codeOrigin.subfolder = gitSubfolder.trim();
  }
}

function setCredentials(credentials) {
  if (credentials === null) {
    return
  }

  if (credentials.username && credentials.passwordOrPAT) {
    model.credentials.username = credentials.username;
    model.credentials.password = credentials.passwordOrPAT;
  } else if (credentials.passwordOrPAT) {
    model.credentials.pat = credentials.passwordOrPAT;
  }
}
