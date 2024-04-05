import * as types from './actionTypes';
import Api from '../../core/Api';
import ApiHandler from '../../core/ApiHandler';
import {beginAjaxCall, ajaxCallError} from './ajaxStatusActions';

export function loadGroupCommentsSuccess(comments){
    return {type: types.LOAD_GROUP_COMMENTS_SUCCESS, comments}
}

export function loadRequestedGroupChats(groupId) {

    return function (dispatch) {

        dispatch(beginAjaxCall());
        let request;
        if(!groupId.header){
           request = ApiHandler.attachHeader({groupId:groupId});
        } else {
            request = groupId
        }

        return Api.post("/GroupService/getPastComments", request)
        .then(apiResponse => {
            let currentComments ='';
            if(apiResponse.data.result){
                 currentComments = apiResponse.data.result.comments.reverse();
            } else {
                currentComments = groupId;
            }
            dispatch(loadGroupCommentsSuccess(currentComments));
            return currentComments;
        }).catch(error => {

            dispatch(ajaxCallError(error));
            throw(error);
        });
    }
}

export function createNewComment(header,id) {

    return function (dispatch) {

        dispatch(beginAjaxCall());
        return Api.post("/GroupService/putComment", header)
            .then(apiResponse => {
             dispatch(loadRequestedGroupChats(id));
             return apiResponse;
        }).catch(error => {
            dispatch(ajaxCallError(error));
            throw(error);
        });
    }
}

export function uploadNewfile(data){
    return function (dispatch) {

        dispatch(beginAjaxCall());

        return Api.post("/ContentService/putContent", data)
            .then(apiResponse => {
                const commentResponse = apiResponse.data.result;
                return commentResponse;
        }).catch(error => {

            dispatch(ajaxCallError(error));
            throw(error);
        });
    }
}

export function deleteComment(request,groupId){
    return function (dispatch) {

        dispatch(beginAjaxCall());
        return Api.post(`/GroupService/deleteComment`, request)
            .then(apiResponse => {
              
                dispatch(loadRequestedGroupChats(groupId));
        }).catch(error => {

            dispatch(ajaxCallError(error));
            throw(error);
        });
    }
}
