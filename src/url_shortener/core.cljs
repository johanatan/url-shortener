(ns url-shortener.core
  (:require [matchbox.core :as m]
            [reagent.core :as reagent]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [secretary.core :as secretary :refer-macros [defroute]])
  (:import [goog.string format]
           goog.History))

(enable-console-print!)

(defonce app-state
  (reagent/atom {:error nil :input "" :produced nil :retrieved nil}))

(def root (m/connect "https://url-shortener-b1779.firebaseio.com"))

(defroute "/:short-url" [short-url]
  (swap! app-state assoc :retrieving true)
  (let [query (.child (.child root "urls") short-url)]
    (m/deref query #(swap! app-state assoc :retrieved (:url %)))))

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event] (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn is-url? [s]
  (try (js/Boolean (js/URL. s))
       (catch js/Error e false)))

(defn get-existing [url cb]
  (let [query (m/equal-to (m/order-by-child (m/get-in root [:urls]) :url) url)
        fname (fnil name "")]
    (m/deref query #(cb (-> % keys first fname)))))

(defn create-new [url]
  (m/conj! (m/get-in root [:urls]) {:url url}
           (fn [res] (swap! app-state assoc :produced (last (.. res -path -u))))))

(defn validate-input []
  (swap! app-state assoc :error (when (not (is-url? (:input @app-state))) "input is not a valid url")))

(defn btn-clicked []
  (validate-input)
  (when (nil? (:error @app-state))
    (get-existing (:input @app-state)
                  (fn [existing]
                    (if (clojure.string/blank? existing)
                      (create-new (:input @app-state))
                      (swap! app-state assoc :produced existing))))))

(def tl-div-style {:style {:margin-top "20px" :margin-left "20px"}})

(defn link [link label]
  [:div
   tl-div-style
   [:label {:style {:margin-right "10px"}} label]
   [:a {:href link} link]])

(defn page []
  (cond
    (not (nil? (:retrieved @app-state)))
    [link (:retrieved @app-state) "Here is your retrieved URL:"]
    (not (nil? (:retrieving @app-state))) nil ; display nothing while retrieval in process
    (not (nil? (:produced @app-state)))
    (let [lnk (format "http://%s%s#%s" js/location.host js/location.pathname (:produced @app-state))]
      [link lnk "Here is your generated URL:"])
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
      [:button {:type "button" :on-click btn-clicked} "Shorten"]]]))

(defn init! []
  (secretary/set-config! :prefix "#")
  (hook-browser-navigation!)
  (reagent/render-component [page] (.getElementById js/document "app")))

(init!)
