import { useEffect, useState } from "react";
import axios from "axios";

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";

import "./App.css";

const API_BASE_URL =
  "http://localhost:8080/api/monitors";

function App() {

  // =====================================================
  // DASHBOARD STATE
  // =====================================================

  const [monitors, setMonitors] = useState([]);

  const [stats, setStats] = useState({});

  const [checkHistory, setCheckHistory] =
    useState({});

  const [alerts, setAlerts] =
    useState({});

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  const [darkMode, setDarkMode] =
    useState(false);

  const [refreshing, setRefreshing] =
    useState(false);

  // =====================================================
  // ADD / EDIT MODAL
  // =====================================================

  const [showMonitorModal, setShowMonitorModal] =
    useState(false);

  const [editingMonitorId, setEditingMonitorId] =
    useState(null);

  const [monitorForm, setMonitorForm] =
    useState({
      name: "",
      url: "",
      method: "GET",
      intervalSeconds: 60,
      timeoutSeconds: 5,
      expectedStatus: 200,
      active: true,
    });

  const [savingMonitor, setSavingMonitor] =
    useState(false);

  const [monitorFormError, setMonitorFormError] =
    useState("");

  // =====================================================
  // INITIAL LOAD
  // =====================================================

  useEffect(() => {
    fetchDashboard();
  }, []);

  // =====================================================
  // AUTO REFRESH
  // =====================================================

  useEffect(() => {

    const interval =
      setInterval(() => {

        fetchDashboard(true);

      }, 10000);

    return () => {
      clearInterval(interval);
    };

  }, []);

  // =====================================================
  // FETCH DASHBOARD
  // =====================================================

  const fetchDashboard = async (
    backgroundRefresh = false
  ) => {

    try {

      if (!backgroundRefresh) {
        setLoading(true);
      }

      setError("");

      if (backgroundRefresh) {
        setRefreshing(true);
      }

      // =================================================
      // GET MONITORS
      // =================================================
        const monitorResponse = await axios.get(
  `${API_BASE_URL}?page=0&size=100`
);

const monitorData = monitorResponse.data;

const monitorList = Array.isArray(monitorData)
  ? monitorData
  : Array.isArray(monitorData?.content)
    ? monitorData.content
    : [];

setMonitors(monitorList);
      // =================================================
      // GET STATISTICS
      // =================================================

      const statsResults =
        await Promise.all(

          monitorList.map(
            async (monitor) => {

              try {

                const response =
                  await axios.get(
                    `${API_BASE_URL}/${monitor.id}/stats`
                  );

                return {
                  id: monitor.id,
                  data: response.data,
                };

              } catch (err) {

                console.error(
                  `Failed to get stats for monitor ${monitor.id}`,
                  err
                );

                return {
                  id: monitor.id,
                  data: null,
                };
              }
            }
          )
        );

      const statsMap = {};

      statsResults.forEach(
        (result) => {

          statsMap[result.id] =
            result.data;

        }
      );

      setStats(statsMap);

      // =================================================
      // GET CHECK HISTORY
      // =================================================

      const historyResults =
        await Promise.all(

          monitorList.map(
            async (monitor) => {

              try {

                const response =
                  await axios.get(
                    `${API_BASE_URL}/${monitor.id}/checks`
                  );

                return {
                   id: monitor.id,
  data: Array.isArray(response.data)
    ? response.data
    : Array.isArray(response.data?.content)
      ? response.data.content
      : [],
                };

              } catch (err) {

                console.error(
                  `Failed to get history for monitor ${monitor.id}`,
                  err
                );

                return {
                  id: monitor.id,
                  data: [],
                };
              }
            }
          )
        );

      const historyMap = {};

      historyResults.forEach(
        (result) => {

          historyMap[result.id] =
            result.data;

        }
      );

      setCheckHistory(historyMap);

      // =================================================
      // GET ALERTS
      // =================================================

      const alertResults =
        await Promise.all(

          monitorList.map(
            async (monitor) => {

              try {

                const response =
                  await axios.get(
                    `${API_BASE_URL}/${monitor.id}/alerts`
                  );

                return {
                  id: monitor.id,
                  data: response.data,
                };

              } catch (err) {

                console.error(
                  `Failed to get alerts for monitor ${monitor.id}`,
                  err
                );

                return {
                  id: monitor.id,
                  data: [],
                };
              }
            }
          )
        );

      const alertsMap = {};

      alertResults.forEach(
        (result) => {

          alertsMap[result.id] =
            result.data;

        }
      );

      setAlerts(alertsMap);

      // =================================================
      // FINISH LOADING
      // =================================================

      setLoading(false);
      setRefreshing(false);

    } catch (err) {

      console.error(err);

      setError(
        "Failed to connect to Spring Boot backend."
      );

      setLoading(false);
      setRefreshing(false);
    }
  };

  // =====================================================
  // MONITOR STATUS
  // =====================================================

  const getMonitorStatus = (
    monitor
  ) => {

    const monitorStats =
      stats[monitor.id];

    if (!monitorStats) {
      return "NO_DATA";
    }

    return monitorStats.status;
  };

  // =====================================================
  // SUMMARY
  // =====================================================

  const activeCount =
    monitors.filter(
      (monitor) =>
        monitor.active
    ).length;

  const inactiveCount =
    monitors.filter(
      (monitor) =>
        !monitor.active
    ).length;

  const upCount =
    monitors.filter(
      (monitor) =>
        stats[monitor.id]?.status === "UP"
    ).length;

  const downCount =
    monitors.filter(
      (monitor) =>
        stats[monitor.id]?.status === "DOWN"
    ).length;

  // =====================================================
  // FORM CHANGE
  // =====================================================

  const handleMonitorChange =
    (event) => {

      const {
        name,
        value,
        type,
        checked,
      } = event.target;

      setMonitorForm(
        (previous) => ({
          ...previous,

          [name]:
            type === "checkbox"
              ? checked
              : value,
        })
      );
    };

  // =====================================================
  // OPEN ADD MONITOR
  // =====================================================

  const openAddMonitor =
    () => {

      setEditingMonitorId(null);

      setMonitorForm({
        name: "",
        url: "",
        method: "GET",
        intervalSeconds: 60,
        timeoutSeconds: 5,
        expectedStatus: 200,
        active: true,
      });

      setMonitorFormError("");

      setShowMonitorModal(true);
    };

  // =====================================================
  // OPEN EDIT MONITOR
  // =====================================================

  const openEditMonitor =
    (monitor) => {

      setEditingMonitorId(
        monitor.id
      );

      setMonitorForm({

        name:
          monitor.name || "",

        url:
          monitor.url || "",

        method:
          monitor.method || "GET",

        intervalSeconds:
          monitor.intervalSeconds ?? 60,

        timeoutSeconds:
          monitor.timeoutSeconds ?? 5,

        expectedStatus:
          monitor.expectedStatus ?? 200,

        active:
          monitor.active ?? true,
      });

      setMonitorFormError("");

      setShowMonitorModal(true);
    };

  // =====================================================
  // CLOSE MODAL
  // =====================================================

  const closeMonitorModal =
    () => {

      if (savingMonitor) {
        return;
      }

      setShowMonitorModal(false);

      setEditingMonitorId(null);

      setMonitorFormError("");
    };

  // =====================================================
  // SAVE MONITOR
  // =====================================================

  const handleSaveMonitor =
    async (event) => {

      event.preventDefault();

      try {

        setSavingMonitor(true);

        setMonitorFormError("");

        const monitorData = {

          name:
            monitorForm.name.trim(),

          url:
            monitorForm.url.trim(),

          method:
            monitorForm.method,

          intervalSeconds:
            Number(
              monitorForm.intervalSeconds
            ),

          timeoutSeconds:
            Number(
              monitorForm.timeoutSeconds
            ),

          expectedStatus:
            Number(
              monitorForm.expectedStatus
            ),

          active:
            monitorForm.active,
        };

        // =================================================
        // EDIT
        // =================================================

        if (
          editingMonitorId !== null
        ) {

          await axios.put(

            `${API_BASE_URL}/${editingMonitorId}`,

            monitorData

          );

        }

        // =================================================
        // CREATE
        // =================================================

        else {

          await axios.post(

            API_BASE_URL,

            monitorData

          );
        }

        setShowMonitorModal(false);

        setEditingMonitorId(null);

        await fetchDashboard();

      } catch (err) {

        console.error(err);

        if (
          err.response?.data?.message
        ) {

          setMonitorFormError(
            err.response.data.message
          );

        } else {

          setMonitorFormError(

            editingMonitorId !== null

              ? "Failed to update monitor. Make sure the Spring Boot backend is running."

              : "Failed to add monitor. Make sure the Spring Boot backend is running."

          );
        }

      } finally {

        setSavingMonitor(false);
      }
    };

  // =====================================================
  // DELETE MONITOR
  // =====================================================

  const handleDeleteMonitor =
    async (monitor) => {

      const confirmed =
        window.confirm(
          `Are you sure you want to delete "${monitor.name}"?`
        );

      if (!confirmed) {
        return;
      }

      try {

        await axios.delete(
          `${API_BASE_URL}/${monitor.id}`
        );

        await fetchDashboard();

      } catch (err) {

        console.error(err);

        alert(
          "Failed to delete monitor."
        );
      }
    };

  // =====================================================
  // PREPARE CHART DATA
  // =====================================================

  const getChartData =
    (monitorId) => {

      const history =
        checkHistory[monitorId] || [];

      return history

        .slice(0, 20)

        .reverse()

        .map(
          (check) => ({

            time:
              new Date(
                check.checkedAt
              ).toLocaleTimeString(),

            responseTime:
              check.responseTimeMs ?? 0,

            success:
              check.success,
          })
        );
    };

  // =====================================================
  // LOADING SCREEN
  // =====================================================

  if (loading) {

    return (

      <div className="loading-screen">

        <div className="loading-spinner">
        </div>

        <h2>
          Loading monitors...
        </h2>

      </div>
    );
  }

  // =====================================================
  // ERROR SCREEN
  // =====================================================

  if (error) {

    return (

      <div className="error-screen">

        <h2>
          {error}
        </h2>

        <button
          className="common-button retry-button"
          onClick={() =>
            fetchDashboard()
          }
        >
          Try Again
        </button>

      </div>
    );
  }

  // =====================================================
  // DASHBOARD
  // =====================================================

  return (

    <div
      className={
        darkMode
          ? "dashboard dark-mode"
          : "dashboard"
      }
    >

      {/* =================================================
          HEADER
      ================================================= */}

      <header className="header">

        <div className="header-title">

          <h1>
            Uptime Monitor
          </h1>

          <p>
            Application Health & Uptime Dashboard
          </p>

        </div>

        <div className="header-actions">

          {/* REFRESH */}

          <button
            className="common-button refresh-button"
            onClick={() =>
              fetchDashboard()
            }
            disabled={refreshing}
          >

            {refreshing
              ? "↻ Refreshing..."
              : "↻ Refresh"}

          </button>

          {/* ADD */}

          <button
            className="common-button add-monitor-button"
            onClick={
              openAddMonitor
            }
          >
            + Add Monitor
          </button>

          {/* THEME */}

          <div className="theme-switch">

            <span className="theme-icon">
              ☀️
            </span>

            <label className="switch">

              <input
                type="checkbox"
                checked={darkMode}
                onChange={() =>
                  setDarkMode(
                    !darkMode
                  )
                }
              />

              <span className="slider">
              </span>

            </label>

            <span className="theme-icon">
              🌙
            </span>

          </div>

        </div>

      </header>

      {/* =================================================
          SUMMARY
      ================================================= */}

      <section className="summary">

        <div className="summary-card">

          <h3>
            Total Monitors
          </h3>

          <p>
            {monitors.length}
          </p>

        </div>

        <div className="summary-card">

          <h3>
            Active
          </h3>

          <p>
            {activeCount}
          </p>

        </div>

        <div className="summary-card">

          <h3>
            Inactive
          </h3>

          <p>
            {inactiveCount}
          </p>

        </div>

        <div className="summary-card">

          <h3>
            UP
          </h3>

          <p className="up-number">
            {upCount}
          </p>

        </div>

        <div className="summary-card">

          <h3>
            DOWN
          </h3>

          <p className="down-number">
            {downCount}
          </p>

        </div>

      </section>

      {/* =================================================
          MONITOR GRID
      ================================================= */}

      <section className="monitor-grid">

        {monitors.map(
          (monitor) => {

            const monitorStats =
              stats[monitor.id];

            const status =
              getMonitorStatus(
                monitor
              );

            const chartData =
              getChartData(
                monitor.id
              );

            return (

              <div
                className="monitor-card"
                key={monitor.id}
              >

                {/* MONITOR HEADER */}

                <div className="monitor-header">

                  <h2>
                    {monitor.name}
                  </h2>

                  <span
                    className={
                      monitor.active
                        ? "status active"
                        : "status inactive"
                    }
                  >

                    {monitor.active
                      ? "ACTIVE"
                      : "INACTIVE"}

                  </span>

                </div>

                {/* CURRENT STATUS */}

                <div
                  className={

                    status === "UP"

                      ? "monitor-status status-up"

                      : status === "DOWN"

                      ? "monitor-status status-down"

                      : "monitor-status"

                  }
                >

                  <span
                    className="status-dot"
                  >
                  </span>

                  <span>
                    {status}
                  </span>

                </div>

                {/* DETAILS */}

                <div className="monitor-details">

                  <p>
                    <strong>
                      URL:
                    </strong>{" "}
                    {monitor.url}
                  </p>

                  <p>
                    <strong>
                      Method:
                    </strong>{" "}
                    {monitor.method}
                  </p>

                  <p>
                    <strong>
                      Interval:
                    </strong>{" "}
                    {monitor.intervalSeconds}
                    {" "}seconds
                  </p>

                  <p>
                    <strong>
                      Timeout:
                    </strong>{" "}
                    {monitor.timeoutSeconds}
                    {" "}seconds
                  </p>

                  <p>
                    <strong>
                      Expected Status:
                    </strong>{" "}
                    {monitor.expectedStatus}
                  </p>

                  <p>
                    <strong>
                      Last Checked:
                    </strong>{" "}

                    {monitor.lastCheckedAt

                      ? new Date(
                          monitor.lastCheckedAt
                        ).toLocaleString()

                      : "Not checked yet"}

                  </p>

                  {/* STATISTICS */}

                  {monitorStats && (

                    <>

                      <p>
                        <strong>
                          Uptime:
                        </strong>{" "}

                        {Number(
                          monitorStats
                            .uptimePercentage
                        ).toFixed(2)}

                        %
                      </p>

                      <p>
                        <strong>
                          Average Response:
                        </strong>{" "}

                        {Number(
                          monitorStats
                            .averageResponseTimeMs
                        ).toFixed(2)}

                        ms
                      </p>

                      <p>
                        <strong>
                          Last Response:
                        </strong>{" "}

                        {monitorStats
                          .lastResponseTimeMs ??
                          "N/A"}

                        {" "}ms
                      </p>

                      <p>
                        <strong>
                          Total Checks:
                        </strong>{" "}

                        {
                          monitorStats
                            .totalChecks
                        }

                      </p>

                      <p>
                        <strong>
                          Successful:
                        </strong>{" "}

                        {
                          monitorStats
                            .successfulChecks
                        }

                      </p>

                      <p>
                        <strong>
                          Failed:
                        </strong>{" "}

                        {
                          monitorStats
                            .failedChecks
                        }

                      </p>

                    </>

                  )}

                </div>

                {/* =================================================
                    RESPONSE TIME CHART
                ================================================= */}

                <div className="chart-section">

                  <div className="chart-header">

                    <h3>
                      Response Time
                    </h3>

                    <span>
                      Last 20 checks
                    </span>

                  </div>

                  {chartData.length > 0 ? (

                    <div className="chart-container">

                      <ResponsiveContainer
                        width="100%"
                        height={220}
                      >

                        <LineChart
                          data={chartData}
                        >

                          <CartesianGrid
                            strokeDasharray="3 3"
                          />

                          <XAxis
                            dataKey="time"
                            tick={{
                              fontSize: 11,
                            }}
                          />

                          <YAxis
                            tick={{
                              fontSize: 11,
                            }}
                          />

                          <Tooltip />

                          <Line
                            type="monotone"
                            dataKey="responseTime"
                            name="Response Time"
                            stroke="#2563eb"
                            strokeWidth={2}
                            dot={false}
                          />

                        </LineChart>

                      </ResponsiveContainer>

                    </div>

                  ) : (

                    <div className="no-chart-data">

                      No check history
                      available.

                    </div>

                  )}

                </div>
                                {/* =================================================
                    ALERT HISTORY
                ================================================= */}

                <div className="alert-section">

                  <div className="alert-header">
                    <h3>Alert History</h3>
                    <span>
                      {(alerts[monitor.id] || []).length} alerts
                    </span>
                  </div>

                  {(alerts[monitor.id] || []).length > 0 ? (

                    <div className="alert-list">

                      {alerts[monitor.id]
                        .slice()
                        .reverse()
                        .map((alert) => (

                          <div
                            className={`alert-item ${
                              alert.status === "FIRING"
                                ? "alert-firing"
                                : "alert-resolved"
                            }`}
                            key={alert.id}
                          >

                            <div className="alert-item-header">

                              <strong>
                                {alert.status}
                              </strong>

                              <span>
                                {new Date(
                                  alert.createdAt
                                ).toLocaleString()}
                              </span>

                            </div>

                            <p>
                              {alert.message}
                            </p>

                            {alert.resolvedAt && (
                              <small>
                                Resolved:{" "}
                                {new Date(
                                  alert.resolvedAt
                                ).toLocaleString()}
                              </small>
                            )}

                          </div>

                        ))}

                    </div>

                  ) : (

                    <div className="no-alerts">
                      No alerts
                    </div>

                  )}

                </div>

                {/* =================================================
                    ACTIONS
                ================================================= */}

                <div className="monitor-card-actions">

                  <button
                    className="common-button edit-button"
                    onClick={() =>
                      openEditMonitor(
                        monitor
                      )
                    }
                  >
                    ✎ Edit
                  </button>

                  <button
                    className="common-button delete-button"
                    onClick={() =>
                      handleDeleteMonitor(
                        monitor
                      )
                    }
                  >
                    🗑 Delete
                  </button>

                </div>

                <button
                  className="common-button statistics-button"
                  onClick={() =>
                    window.open(
                      `${API_BASE_URL}/${monitor.id}/stats`,
                      "_blank"
                    )
                  }
                >
                  View Statistics
                </button>

              </div>
            );
          }
        )}

      </section>

      {/* =================================================
          ADD / EDIT MODAL
      ================================================= */}

      {showMonitorModal && (

        <div
          className="modal-overlay"
          onClick={
            closeMonitorModal
          }
        >

          <div
            className="add-monitor-modal"
            onClick={(event) =>
              event.stopPropagation()
            }
          >

            {/* MODAL HEADER */}

            <div className="modal-header">

              <div>

                <h2>

                  {editingMonitorId !== null
                    ? "Edit Monitor"
                    : "Add New Monitor"}

                </h2>

                <p>

                  {editingMonitorId !== null
                    ? "Update monitor configuration."
                    : "Configure a website or API to monitor."}

                </p>

              </div>

              <button
                className="modal-close-button"
                onClick={
                  closeMonitorModal
                }
                disabled={
                  savingMonitor
                }
              >
                ×
              </button>

            </div>

            {/* FORM */}

            <form
              onSubmit={
                handleSaveMonitor
              }
              className="monitor-form"
            >

              {/* NAME */}

              <div className="form-group">

                <label>
                  Monitor Name
                </label>

                <input
                  type="text"
                  name="name"
                  value={
                    monitorForm.name
                  }
                  onChange={
                    handleMonitorChange
                  }
                  placeholder="Example Site"
                  required
                />

              </div>

              {/* URL */}

              <div className="form-group">

                <label>
                  URL
                </label>

                <input
                  type="url"
                  name="url"
                  value={
                    monitorForm.url
                  }
                  onChange={
                    handleMonitorChange
                  }
                  placeholder="https://example.com"
                  required
                />

              </div>

              {/* METHOD */}

              <div className="form-group">

                <label>
                  HTTP Method
                </label>

                <select
                  name="method"
                  value={
                    monitorForm.method
                  }
                  onChange={
                    handleMonitorChange
                  }
                >

                  <option value="GET">
                    GET
                  </option>

                  <option value="HEAD">
                    HEAD
                  </option>

                </select>

              </div>

              {/* INTERVAL / TIMEOUT */}

              <div className="form-row">

                <div className="form-group">

                  <label>
                    Interval (seconds)
                  </label>

                  <input
                    type="number"
                    name="intervalSeconds"
                    value={
                      monitorForm
                        .intervalSeconds
                    }
                    onChange={
                      handleMonitorChange
                    }
                    min="1"
                    required
                  />

                </div>

                <div className="form-group">

                  <label>
                    Timeout (seconds)
                  </label>

                  <input
                    type="number"
                    name="timeoutSeconds"
                    value={
                      monitorForm
                        .timeoutSeconds
                    }
                    onChange={
                      handleMonitorChange
                    }
                    min="1"
                    required
                  />

                </div>

              </div>

              {/* EXPECTED STATUS */}

              <div className="form-group">

                <label>
                  Expected HTTP Status
                </label>

                <input
                  type="number"
                  name="expectedStatus"
                  value={
                    monitorForm
                      .expectedStatus
                  }
                  onChange={
                    handleMonitorChange
                  }
                  min="100"
                  max="599"
                  required
                />

              </div>

              {/* ACTIVE */}

              <div className="active-checkbox">

                <input
                  type="checkbox"
                  id="active"
                  name="active"
                  checked={
                    monitorForm.active
                  }
                  onChange={
                    handleMonitorChange
                  }
                />

                <label htmlFor="active">
                  Monitor is active
                </label>

              </div>

              {/* FORM ERROR */}

              {monitorFormError && (

                <div className="form-error">
                  {monitorFormError}
                </div>

              )}

              {/* BUTTONS */}

              <div className="modal-actions">

                <button
                  type="button"
                  className="common-button cancel-button"
                  onClick={
                    closeMonitorModal
                  }
                  disabled={
                    savingMonitor
                  }
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="common-button submit-monitor-button"
                  disabled={
                    savingMonitor
                  }
                >

                  {savingMonitor

                    ? "Saving..."

                    : editingMonitorId !== null

                    ? "Save Changes"

                    : "Add Monitor"}

                </button>

              </div>

            </form>

          </div>

        </div>

      )}

    </div>
  );
}

export default App;