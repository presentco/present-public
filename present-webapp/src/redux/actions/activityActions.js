
import * as types from './actionTypes';
import Api from '../../core/Api';
import {beginAjaxCall, ajaxCallError} from './ajaxStatusActions';


export function loadNotificationsSuccess(notifications) {
    return {type: types.LOAD_NOTIFICATION_SUCCESS, notifications}
}

export function loadInboxSuccess(inbox) {
    return {type: types.LOAD_INBOX_SUCCESS, inbox}
}



export function getUserNotifications(data){
  return function (dispatch) {

      dispatch(beginAjaxCall());
      return Api.post("/ActivityService/getPastActivity", data)
          .then(apiResponse => {
            let notifications = apiResponse.data.result.events;
            dispatch(loadNotificationsSuccess(notifications));
            return notifications;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function getUserInbox(data){
  return function (dispatch) {

      dispatch(beginAjaxCall());
      return Api.post("/MessagingService/getChats", data)
          .then(apiResponse => {
            let inbox = apiResponse.data.result.chats;
            dispatch(loadInboxSuccess(inbox));
            return inbox;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}
