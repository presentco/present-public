import * as types from './actionTypes';
import Api from '../../core/Api';
import {beginAjaxCall, ajaxCallError} from './ajaxStatusActions';
import _ from 'lodash';

export function loadSavedGroupSuccess(groups){
    return {type: types.LOAD_SAVED_GROUP_SUCCESS, groups}
}

export function loadNearbyGroupsSuccess(groups){
  return {type: types.LOAD_NEARBY_GROUPS_SUCCESS, groups}
}

export function getSavedGroups(data, userHome){
  if(userHome && userHome.location){
    data.argument.location = userHome.location;
  }

  try {
      const jsonCurrentCity = localStorage.getItem('selectedCity');

      if (jsonCurrentCity !== undefined) {
          const currentCity = JSON.parse(jsonCurrentCity)
          if (currentCity) {
            data.argument.location = currentCity.location
          }
      }
  } catch (e) {
      // localStorage.removeItem('currentUser');
  }

    return function (dispatch) {
        dispatch(beginAjaxCall());
        userHome && userHome === "markRead" ? {} : dispatch(getEventsNearBy(data));
        return Api.post("/GroupService/getSavedGroups", data)
            .then(apiResponse => {
                const savedGroups = apiResponse.data.result;
                dispatch(loadSavedGroupSuccess(savedGroups));
                return savedGroups;
        }).catch(error => {
            dispatch(ajaxCallError(error));
            throw(error);
        });
    }
}

export function getOtherUserSavedGroups(userId){
  return function (dispatch) {
      dispatch(beginAjaxCall());
      return Api.post("/GroupService/getSavedGroups", userId)
        .then(apiResponse => {
          const savedGroups = apiResponse.data.result;
          return savedGroups;
      }).catch(error => {
          dispatch(ajaxCallError(error));
          throw(error);
      });
  }
}

export function getCities(data){

    return function (dispatch) {
        dispatch(beginAjaxCall());
        return Api.post("/GroupService/getCities", data)
          .then(apiResponse => {
            const cities = apiResponse.data.result.cities;
            return cities;
        }).catch(error => {
            dispatch(ajaxCallError(error));
            throw(error);
        });
    }
}


export function getEventsNearBy(request){

  if(_.isEmpty(request.argument.location)){
    try {
        const jsonCurrentCity = localStorage.getItem('currentUser');

        if (jsonCurrentCity !== undefined) {
            const currentCity = JSON.parse(jsonCurrentCity)
            if (currentCity) {
              request.argument.location = currentCity.home.location
            }
        }
    } catch (e) {
        // localStorage.removeItem('currentUser');
    }
  }
  //we dont want to call this everytime we go there
    return function (dispatch) {
        return Api.post("/GroupService/getNearbyGroups", request)
          .then(apiResponse => {

            //reload if after sometime there was no data
                setTimeout(() =>{
                  if(!apiResponse.data){
                    window.location.reload();
                  }
                },3000);
                const nearbyGroups = apiResponse.data.result.nearbyGroups;
                dispatch(loadNearbyGroupsSuccess(nearbyGroups));
                clearTimeout();
                return nearbyGroups;
        }).catch(error => {

            dispatch(ajaxCallError(error));
            throw(error);
        });
    }
}
