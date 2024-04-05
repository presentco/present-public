import * as types from './actionTypes';
import Api from '../../core/Api';
import ApiHandler from '../../core/ApiHandler';
import {beginAjaxCall, ajaxCallError} from './ajaxStatusActions';

export function loadGroupSuccess(group) {
    return {type: types.LOAD_GROUP_SUCCESS, group}
}


export function loadSocketUrl(host){
  return {type: types.LOAD_SOCKET_SUCCESS, host}
}

export function loadMemberRequests(requests){
  return {type: types.LOAD_MEMBER_REQUESTS, requests}
}

export function loadRequestedGroup(groupId) {

    return function (dispatch) {
        dispatch(beginAjaxCall());
        return Api.post("/GroupService/getGroup", groupId)
            .then(apiResponse => {
                const currentGroup = apiResponse.data.result;
                dispatch(loadGroupSuccess(currentGroup));
                return currentGroup;
        }).catch(error => {
            dispatch(ajaxCallError(error));
            throw(error);
        });
    }
}

export function addMembers(groupId) {
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/GroupService/addMembers", groupId)
          .then(apiResponse => {
              const currentGroup = apiResponse.data.result;
              return currentGroup;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function removeMembers(groupId) {
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/GroupService/removeMembers", groupId)
          .then(apiResponse => {
              const currentGroup = apiResponse.data.result;
              return currentGroup;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function saveGroupRequest(groupId){
    return function (dispatch) {
        dispatch(beginAjaxCall());
        return Api.post("/GroupService/saveGroup", groupId)
            .then(apiResponse => {
              return apiResponse.data;
        }).catch(error => {
            dispatch(ajaxCallError(error));
            throw(error);
        });
    }
}


export function leaveGroupRequest(groupId){
    return function (dispatch) {
        dispatch(beginAjaxCall());

        return Api.post("/GroupService/unsaveGroup", groupId)
            .then(apiResponse => {
             return apiResponse.data;
        }).catch(error => {
            dispatch(ajaxCallError(error));
            throw(error);
        });
    }
}

export function getGroupInfo(url){

  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/UrlResolverService/resolveUrl", url)
          .then(apiResponse => {

            let res;
            if(apiResponse.data.result.group){
              const currentGroup = apiResponse.data.result.group;
              dispatch(loadGroupSuccess(currentGroup));

              if(localStorage.getItem('authResponse')){
                dispatch(findLiveServer(currentGroup.uuid));
              }
               return currentGroup;
            } else if(apiResponse.data.result.user){
              res = apiResponse.data.result.user;
            } else if(apiResponse.data.result.app){
              res = apiResponse.data.result.app;
            } else if(apiResponse.data.result.category){
              res = apiResponse.data.result.category;
            }
            return res;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function getMembershipRequests(uuid){
  let request = ApiHandler.attachHeader({groupId:uuid});
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/GroupService/getMembershipRequests", request)
      .then(apiResponse => {
          let requests = apiResponse.data.result.requests;
          dispatch(loadMemberRequests(requests))
          return requests;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
    }

}

export function muteGroup(groupId){
  return function (dispatch) {
      dispatch(beginAjaxCall());

      return Api.post("/GroupService/muteGroup", groupId)
          .then(apiResponse => {
           return apiResponse;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function markRead(request){
  return function (dispatch) {
      dispatch(beginAjaxCall());

      return Api.post("/GroupService/markRead", request)
          .then(apiResponse => {
           return apiResponse;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function unMuteGroup(groupId){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/GroupService/unMuteGroup", groupId)
          .then(apiResponse => {
           return apiResponse;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function flagGroup(reason){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/GroupService/flagGroup", reason)
          .then(apiResponse => {
           return apiResponse;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function updateGroupInfo(info){

  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/GroupService/putGroup", info)
          .then(apiResponse => {
            const currentGroup = apiResponse.data.result.group;
            dispatch(loadGroupSuccess(currentGroup));
            return currentGroup;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function getGroupMembers(groupId, url){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/GroupService/getGroupMembers", groupId)
          .then(apiResponse => {
            const members = apiResponse.data;
            return members;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function findLiveServer(groupId){
  let request = ApiHandler.attachHeader({groupId:groupId});
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/GroupService/findLiveServer", request)
          .then(apiResponse => {
            const host = apiResponse.data.result;
            dispatch(loadSocketUrl(host));
            return host;
      }).catch(error => {

          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function deleteGroup(groupId){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/GroupService/deleteGroup", groupId)
          .then(apiResponse => {
           return apiResponse;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function countGroupReferrals(){
  let request = ApiHandler.attachHeader({});
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/GroupService/getGroupReferrals", request)
          .then(apiResponse => {
           return apiResponse.data.result.referrals;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function inviteFriends(data){

  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/GroupService/inviteFriends", data)
          .then(apiResponse => {
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function reassignGroup(data){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/GroupService/reassignGroup", data)
          .then(apiResponse => {
            return apiResponse;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}
