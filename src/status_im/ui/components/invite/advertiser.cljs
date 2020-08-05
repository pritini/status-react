(ns status-im.ui.components.invite.advertiser
  (:require [status-im.ui.components.invite.modal :as modal]
            [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
            [status-im.acquisition.advertiser :as advertiser]))

(defn accept-popover []
  [modal/popover {:on-accept    #(re-frame/dispatch [::advertiser/decision :accept])
                  :on-decline   #(re-frame/dispatch [::advertiser/decision :decline])
                  :accept-label (i18n/label :t/advertiser-starter-pack-accept)
                  :title        (i18n/label :t/advertiser-starter-pack-title)
                  :description  (i18n/label :t/advertiser-starter-pack-description)}])
