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
    (m/conj! (m/get-in root [:forward]) (:input @app-state)
            (fn [res]
              (let [id (last (.. res -path -u))]
                (m/conj! (m/get-in root [:reverse])
                         {:id id
                          :path (:input @app-state)}
                         (fn [_]
                           (swap! app-state assoc :produced id))))))))

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
