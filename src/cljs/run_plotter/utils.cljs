(ns run-plotter.utils
  (:require
    [goog.string :as gstring]
    [react-leaflet :as react-leaflet]))

; Assumes the leaflet library has been included
(defn distance-between-lat-lngs
  [[lat1 lng1] [lat2 lng2]]
  (.distanceTo (js/L.latLng lat1 lng1)
               (js/L.latLng lat2 lng2)))

(defn format-distance
  ([distance-in-meters units]
   (format-distance distance-in-meters units 2 false))
  ([distance-in-meters units decimal-places show-units?]
   (let [value-in-km (/ distance-in-meters 1000)
         value (if (= units :miles)
                 (* value-in-km 0.621371)
                 value-in-km)
         formatted-value (gstring/format (str "%." decimal-places "f") value)]
     (if show-units?
       (str formatted-value " " (name units))
       formatted-value))))