
import * as types from './actionTypes';
import Api from '../../core/Api';
import {beginAjaxCall, ajaxCallError} from './ajaxStatusActions';
import ApiHandler from '../../core/ApiHandler';

export function loginSuccess(user) {
    return {type: types.USER_LOGIN_SUCCESS, user}
}

export function logoutSuccess() {
    return {type: types.USER_LOGOUT_SUCCESS}
}

export function loadCurrentUser(user) {
    return function (dispatch) {
        dispatch(beginAjaxCall());
        return Api.post("/UserService/getUserProfile", user)
            .then(apiResponse => {
                const currentUser = apiResponse.data.result;
                dispatch(loginSuccess(currentUser));
                saveUserInLocalStorage(currentUser);
                return currentUser;
        }).catch(error => {
            dispatch(ajaxCallError(error));
            throw(error);
        });
    }
}

export function getFriends(userId) {
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/UserService/getFriends", userId)
        .then(apiResponse => {
          const myFriends = apiResponse.data.result.users;
          return myFriends;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function getFacebookFriends(request) {
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/UserService/getFacebookFriends", request)
        .then(apiResponse => {
          const myFriends = apiResponse.data.result.users;
          return myFriends;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function addFriend(userId) {
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/UserService/addFriend", userId)
          .then(apiResponse => {
              const friends = apiResponse.data.result;
              return friends;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function removeFriend(userId) {
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/UserService/removeFriend", userId)
          .then(apiResponse => {
              const friends = apiResponse.data.result;
              return friends;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function getOutgoingFriendRequests(request) {

  return function (dispatch) {
      dispatch(beginAjaxCall());

      return Api.post("/UserService/getOutgoingFriendRequests", request)
          .then(apiResponse => {

              const friends = apiResponse.data.result.users;
              return friends;
      }).catch(error => {

          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function getIncomingFriendRequests(request) {

  return function (dispatch) {
      dispatch(beginAjaxCall());

      return Api.post("/UserService/getIncomingFriendRequests", request)
          .then(apiResponse => {

              const friends = apiResponse.data.result.users;
              // dispatch(loadFriendsRequestsSuccess(friends));
              return friends;
      }).catch(error => {

          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}



export function verifyPhoneNumber(request){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post('/UserService/requestVerification', request).then( response => {
        return response.data.result;
      }).catch(error => {
        throw(error);
      });
  }
}

export function verifyCode(request){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post('/UserService/verify', request).then( response => {
        return response.data.result;
      }).catch(error => {
        throw(error);
      });
  }
}


export function verifyUser(url) {
    return function (dispatch) {
        dispatch(beginAjaxCall());
        return Api.post("/UserService/verify", url)
            .then(apiResponse => {
                const currentUser = apiResponse.data.result.userProfile;
                dispatch(loginSuccess(currentUser));
                saveUserInLocalStorage(currentUser);
                return apiResponse.data.result;
        }).catch(error => {
            dispatch(ajaxCallError(error));
            throw(error);
        });
    }
}

function saveUserInLocalStorage(user) {
    localStorage.setItem('currentUser', JSON.stringify(user));
}

export function login(userData) {

    return function (dispatch) {
        dispatch(beginAjaxCall());
        return Api.post("/UserService/linkFacebook", userData)
            .then(apiResponse => {
                const currentUser = apiResponse.data.result.userProfile;
                //  dispatch(loginSuccess(currentUser));
                //  saveUserInLocalStorage(currentUser);
                return currentUser ; //ex: {user: ..., isFirstLogin: true}
        }).catch(error => {
          console.log(error);
            dispatch(ajaxCallError(error));
            throw(error);
        });
    }
}

export function synchronize(userData) {

    return function (dispatch) {
        dispatch(beginAjaxCall());

        return Api.post("/UserService/synchronize", userData)
            .then(apiResponse => {

                const authorization = apiResponse.data.result.authorization;
                return authorization;

        }).catch(error => {
          console.log(error);
            dispatch(ajaxCallError(error));
            throw(error);
        });
    }
}

export function putUserPhoto(userPhoto){
  return function (dispatch) {

      dispatch(beginAjaxCall());
      let request = ApiHandler.attachHeader({});
      return Api.post("/UserService/putUserPhoto", userPhoto)
          .then(apiResponse => {
            dispatch(loadCurrentUser(request));
            return apiResponse;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function getSpaces(){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      let request = ApiHandler.attachHeader({});
      return Api.post("/UserService/getSpaces", request)
          .then(apiResponse => {
            return apiResponse.data.result.spaces;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function putUserProfile(userData){
  return function (dispatch) {

      dispatch(beginAjaxCall());
      let request = ApiHandler.attachHeader({});

      return Api.post("/UserService/putUserProfile", userData)
          .then(apiResponse => {

            dispatch(loadCurrentUser(request));
            return apiResponse;
      }).catch(error => {
        
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function completeSignUp(userData){
  return function (dispatch) {

      dispatch(beginAjaxCall());
      return Api.post("/UserService/completeSignup", userData)
          .then(apiResponse => {

            const authorization = apiResponse.data.result.authorization;
            return authorization;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function loadRequestedUser(userId){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/UserService/getUser", userId)
          .then(apiResponse => {
            const user = apiResponse.data.result;
            return user;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function userSearchRequest(text){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/UserService/search", text)
          .then(apiResponse => {
            const users = apiResponse.data.result.users;
            return users;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function blockUser(user){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/UserService/blockUser", user)
          .then(apiResponse => {
            return apiResponse
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function unBlockUser(user){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/UserService/unblockUser", user)
          .then(apiResponse => {
            return apiResponse
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function getBlockedUsers(user){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/UserService/getBlockedUsers", user)
          .then(apiResponse => {
            return apiResponse.data.result.users;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function deleteAccount(user){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/UserService/deleteAccount", user)
          .then(apiResponse => {
            return apiResponse
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function getValidStateTransitions(user){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/UserService/getValidStateTransitions", user)
          .then(apiResponse => {
            return apiResponse
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function transitionState(user){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/UserService/transitionState", user)
          .then(apiResponse => {
            return apiResponse
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function getFollowing(user){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/UserService/getFollowing", user)
          .then(apiResponse => {

            return apiResponse.data.result.users;
      }).catch(error => {

          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function getFollowers(user){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/UserService/getFollowers", user)
          .then(apiResponse => {
            return apiResponse.data.result.users;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function follow(user){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/UserService/follow", user)
          .then(apiResponse => {

            return apiResponse;
      }).catch(error => {

          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function unfollow(user){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/UserService/unfollow", user)
          .then(apiResponse => {
            return apiResponse;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function logout() {
    return function (dispatch) {
        dispatch(beginAjaxCall());
        dispatch(logoutSuccess());
        localStorage.removeItem('currentUser');
        localStorage.removeItem('clientUuid');
        localStorage.removeItem('authResponse');
        localStorage.removeItem('nearByChecked');
        localStorage.removeItem('selectedCity');
        localStorage.removeItem('selectedSpace');
      }
}
