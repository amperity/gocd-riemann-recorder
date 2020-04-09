(ns amperity.gocd.riemann.recorder.plugin
  "Core plugin implementation."
  (:require
    [amperity.gocd.riemann.recorder.logging :as log]
    [amperity.gocd.riemann.recorder.record :as record]
    [amperity.gocd.riemann.recorder.settings :as settings]
    [amperity.gocd.riemann.recorder.util :as u]
    [clojure.java.io :as io]
    [clojure.string :as str])
  (:import
    (com.thoughtworks.go.plugin.api
      GoApplicationAccessor
      GoPluginIdentifier)
    (com.thoughtworks.go.plugin.api.exceptions
      UnhandledRequestTypeException)
    (com.thoughtworks.go.plugin.api.request
      DefaultGoApiRequest
      GoPluginApiRequest)
    (com.thoughtworks.go.plugin.api.response
      DefaultGoPluginApiResponse
      GoPluginApiResponse)
    java.time.Instant))


;; ## Scheduler Initialization

(def *go-accessor* (atom nil))


(defn initialize!
  "Initialize the plugin scheduler state, returning a configured agent."
  [logger go-accessor]
  (alter-var-root #'log/logger (constantly logger))
  (reset! *go-accessor* go-accessor)
  (settings/initialize! go-accessor))



;; ## Request Handling

(defmulti handle-request
  "Handle a plugin API request and respond. Methods should return `true` for an
  empty success response, a data structure to coerce into a successful JSON
  response, or a custom `GoPluginApiResponse`."
  (fn dispatch
    [req-name data]
    req-name))


(defmethod handle-request :default
  [req-name _]
  (throw (UnhandledRequestTypeException. req-name)))


(defn handler
  "Request handling entry-point."
  [^GoPluginApiRequest request]
  (try
    (let [req-name (.requestName request)
          req-data (when-not (str/blank? (.requestBody request))
                     (u/json-decode-map (.requestBody request)))
          result (handle-request req-name req-data)]
      (cond
        (true? result)
        (DefaultGoPluginApiResponse/success "")

        (instance? GoPluginApiResponse result)
        result

        :else
        (DefaultGoPluginApiResponse/success (u/json-encode result))))
    (catch UnhandledRequestTypeException ex
      (throw ex))
    (catch Exception ex
      (log/errorx ex "Failed to process %s plugin request%s"
                  (.requestName request)
                  (when-let [data (not-empty (ex-data ex))]
                    (str " " (pr-str data))))
      (DefaultGoPluginApiResponse/error (.getMessage ex)))))



;; ## Plugin Metadata

;; Tells the server what types of notifications the plugin is interested in.
;; Currently we only support `stage-status`.
(defmethod handle-request "notifications-interested-in"
  [_ _]
  {:notifications ["stage-status"]})



;; ## Plugin Configuration

;; Returns HTML used to configure the plugin in the GoCD plugin admin interface.
(defmethod handle-request "go.plugin-settings.get-view"
  [_ _]
  (log/debug "get-view request")
  (settings/get-view))


;; Returns the current plugin configuration object.
(defmethod handle-request "go.plugin-settings.get-configuration"
  [_ _]
  (log/debug "get-configuration request")
  (settings/get-configuration))


;; Validates the input plugin config. Returns an array containing maps of
;; `{:key "key-str" :message "message-str}` error messages from the proposed
;; configuration.
(defmethod handle-request "go.plugin-settings.validate-configuration"
  [_ data]
  (log/debug "validate-configuration: %s" (pr-str data))
  (settings/validate-configuration data))


(defmethod handle-request "go.plugin-settings.plugin-settings-changed"
  [_ data]
  (log/debug "plugin-settings-changed: %s" (pr-str data))
  (settings/plugin-settings-changed! data))



;; ## Recorder

;; Record information about a stage.
(defmethod handle-request "stage-status"
  [_ data]
  (record/stage-status data))
