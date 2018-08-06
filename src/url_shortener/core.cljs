(ns url-shortener.core
  (:require [matchbox.core :as m]
            [reagent.core :as reagent])
  (:import [goog.string format]))

(enable-console-print!)

(defonce app-state
  (reagent/atom {:error nil :input ""}))

(def root (m/connect "https://url-shortener-b1779.firebaseio.com"))

(defn is-url? [s]
  (try (js/Boolean (js/URL. s))
       (catch js/Error e false)))

(defn validate-input []
  (swap! app-state assoc :error (when (not (is-url? (:input @app-state)))
                                  "input is not a valid url")))

(defn str->buf [str]
  (let [enc (js/window.TextEncoder.)
        encoded (.encode enc str)]
    (.-buffer encoded)))

(defn buf->str [buf]
  (let [dec (js/window.TextDecoder. "utf-16")
        arr (js/Uint16Array. buf)]
    (.decode dec arr)))

(defn btn-clicked []
  (validate-input)
  (when (nil? (:error @app-state))
    (let [prom (.digest js/window.crypto.subtle "SHA-256" (str->buf (:input @app-state)))]
      (.then prom
             (fn [val]
               (js/console.log (buf->str val)))))))

(defn page []
  [:div
   (when (not (clojure.string/blank? (:error @app-state)))
     [:div
      {:style {:background-color "red"
               :width "380px"}}
      [:label (format "Error:  %s" (:error @app-state))]])
   [:div
    {:style {:margin-top "20px" :margin-left "20px"}}
    [:label {:for "url-input"} "Enter the URL:"]
    [:input {:type "text" :on-input #(do (swap! app-state assoc :input (-> %1 .-target .-value))
                                         (validate-input))
             :style {:width "200px"}
             :id "url-input" :value (:input @app-state) :autoFocus true}]
    [:button {:type "button" :on-click btn-clicked} "Shorten"]]])

(reagent/render-component [page] (.getElementById js/document "app"))
