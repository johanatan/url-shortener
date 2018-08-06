(ns url-shortener.core
  (:require [matchbox.core :as m]
            [reagent.core :as reagent])
  (:import [goog.string format]))

(enable-console-print!)

(defonce app-state
  (reagent/atom {:error nil :input "" :produced nil}))

(def root (m/connect "https://url-shortener-b1779.firebaseio.com"))

(defn is-url? [s]
  (try (js/Boolean (js/URL. s))
       (catch js/Error e false)))

(defn validate-input []
  (swap! app-state assoc :error (when (not (is-url? (:input @app-state))) "input is not a valid url")))

(defn btn-clicked []
  (validate-input)
  (when (nil? (:error @app-state))
    (m/conj! (m/get-in root [:urls]) (:input @app-state)
            (fn [res] (swap! app-state assoc :produced (last (.. res -path -u)))))))

(defn page []
  (let [tl-div-style {:style {:margin-top "20px" :margin-left "20px"}}]
    (cond
      (not (nil? (:produced @app-state)))
      (let [link (format "http://%s/%s" js/window.location.host (:produced @app-state))]
        [:div
         tl-div-style
         [:label {:style {:margin-right "10px"}} "Here is your URL:"]
         [:a {:href link} link]])
      :else
      [:div
       (when (not (clojure.string/blank? (:error @app-state)))
         [:div
          {:style {:background-color "red"
                   :width "380px"}}
          [:label (format "Error:  %s" (:error @app-state))]])
       [:div
        tl-div-style
        [:label {:style {:margin-right"10px"} :for "url-input"} "Enter the URL:"]
        [:input {:type "text" :on-input #(do (swap! app-state assoc :input (-> %1 .-target .-value))
                                             (validate-input))
                 :style {:width "200px"}
                 :id "url-input" :value (:input @app-state) :autoFocus true}]
        [:button {:type "button" :on-click btn-clicked} "Shorten"]]])))

(reagent/render-component [page] (.getElementById js/document "app"))
