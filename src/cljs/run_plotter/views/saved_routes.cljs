(ns run-plotter.views.saved-routes
  (:require
    [re-frame.core :as re-frame]
    [run-plotter.subs :as subs]
    [reagent.core :as reagent]
    [com.michaelgaare.clojure-polyline :as polyline]))

(defn- draw-tile-layer
  [map-obj]
  (-> (js/L.tileLayer "http://{s}.tile.osm.org/{z}/{x}/{y}{r}.png"
                      (clj->js {:attribution "© OpenStreetMap contributors"}))
      (.addTo map-obj)))

(def ^:private polyline-styles
  {:color "grey"
   :dashArray "4"})

(defn- route->leaflet-polyline
  [{:keys [polyline]}]
  (js/L.polyline (clj->js (polyline/decode polyline))
                 (clj->js polyline-styles)))

(defn- draw-routes
  [polylines leaflet-map]
  (doseq [polyline polylines]
    (.addTo polyline leaflet-map)))

(defn- highlight-polyline
  [map-atom polylines-by-id id]
  (let [polyline (polylines-by-id id)
        other-polylines (vals (dissoc polylines-by-id id))]
    (.fitBounds @map-atom (.getBounds polyline))
    (.setStyle polyline #js {:color "red"
                             :dashArray "none"})
    (.bringToFront polyline)
    (doseq [p other-polylines]
      (.setStyle p (clj->js polyline-styles)))))

(defn map-component
  []
  (let [map-obj (atom nil)]
    (reagent/create-class
      {:reagent-render (fn [] [:div#map {:style {:height "80vh"}}])
       :component-did-mount (fn [component]
                              (let [{:keys [polylines on-map-render]} (reagent/props component)
                                    initial-lat-lng #js [51.437382 -2.590950]
                                    leaflet-map (js/L.map "map" #js {:center initial-lat-lng
                                                                     :zoom 14})]
                                (draw-tile-layer leaflet-map)
                                (draw-routes polylines leaflet-map)
                                (on-map-render leaflet-map)
                                (reset! map-obj leaflet-map)))
       :component-did-update (fn [component]
                               (let [{:keys [polylines]} (reagent/props component)]
                                 (draw-routes polylines @map-obj)))})))

(defn saved-routes-panel []
  ; Note - in this component, both the map itself and the route list need
  ; to access the JS objects for the leaflet map and polylines. These feel
  ; like they belong as local state to this component rather than the re-frame
  ; app db, so it uses an atom to hold the local state, as suggested here:
  ; https://reagent-project.github.io/ ->> Managing state in Reagent
  (let [map-atom (atom nil)]
    (fn render-fn []
      (let [on-map-render #(reset! map-atom %)
            routes (re-frame/subscribe [::subs/saved-routes])
            polylines-by-id (reduce (fn [polylines {:keys [id] :as route}]
                                      (assoc polylines id (route->leaflet-polyline route)))
                                    {}
                                    @routes)]
        [:div.columns
         [:div.column
          [map-component {:on-map-render on-map-render
                          :polylines (vals polylines-by-id)}]]
         [:div.column.is-one-third
          [:h1.title.is-3 "Saved routes:"]
          (for [{:keys [id name distance]} @routes]
            ^{:key id}
            [:div.columns.saved-route-column
             {:on-mouse-over (fn [_] (highlight-polyline map-atom polylines-by-id id))}
             [:div.column (str "Route " id ", distance " distance)]
             [:div.column
              [:button
               {:on-click (fn [_] (re-frame/dispatch [:delete-route id]))}
               "X"]]])]]))))