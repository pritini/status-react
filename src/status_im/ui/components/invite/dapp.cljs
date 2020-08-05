(ns status-im.ui.components.invite.dapp
  (:require [status-im.ui.components.invite.modal :as modal]
            [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
            [status-im.acquisition.dapp :as dapp]))

(defn accept-popover []
  [modal/popover {:on-accept    #(re-frame/dispatch [::dapp/decision :accept])
                  :on-decline   #(re-frame/dispatch [::dapp/decision :decline])
                  :accept-label (i18n/label :t/dapp-starter-pack-accept)
                  :title        (i18n/label :t/dapp-starter-pack-title)
                  :description  (i18n/label :t/dapp-starter-pack-description)}])


