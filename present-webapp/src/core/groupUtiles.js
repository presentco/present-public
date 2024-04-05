import _ from 'lodash';

let groupUtiles = (function () {

  let filterGroups = function (interest, nearbyGroups) {

    switch (interest) {
      case "Communities":
        interest = "Organize"
        break;
      case "Style":
        interest = "Shop"
        break;
      case "Events":
        interest = "Attend"
        break;
      case "Woman Owned":
        interest = "Women-Only"
        break;
      default:

    }

    let desiredGroups = [];
    if(!_.isEmpty(nearbyGroups)){
      for (let group of nearbyGroups) {
        if(group.categories.length !== 0){
          for (let category of group.categories) {
            if (category === interest){
            desiredGroups.push(group);
            }
          }
        }
      }
    }

    return desiredGroups
  };

  let getGroupUrl = (function(group){

    let url = `/g/${group.url.split('g/')[1]}`;
    return url;
  })


  let getUserCreatedCircles = function(user, savedGroups){
    let createdCircles = [];
    for(let group of savedGroups){
      if (group.owner.id === user.id){
        createdCircles.push(group);
      }
    }

    return createdCircles;
  }

  let getUserOnlyJoinedCircles = function(user, savedGroups){
    let joinedCircles = [];
    for(let group of savedGroups){
      if (group.owner.id !== user.id){
        joinedCircles.push(group);
      }
    }

    return joinedCircles;
  }

  let isGroupMuted = function(mutedGroups, currentGroup){

    let muted=[];
    for (let mutedGroup of mutedGroups.groupIds) {
      if(currentGroup.uuid === mutedGroup){
        muted.push(true);
      } else{
        muted.push(false);
      }
    }
    return muted.indexOf(true) > -1;
  }

  let getAllChatMedia = function(comments){
    let media = [];

    comments.map(comment => {
      if(comment.content){
        media.push(comment);
      }
    });

    return media;
  }

  let getNearBySavedGroups = function(nearByGroups, savedGroups){
    let nearbyOnly = [];
    for(let circle of savedGroups){
      for(let nearby of nearByGroups){
        if(nearby.uuid === circle.uuid){
          nearbyOnly.push(circle);
        }
      }
    }
    return nearbyOnly;
  }

  let getSelectedCity = function(userHome){
    //if nothing selected San Francisco will be default one
    let newLocation = userHome;
    // else get selected city info
    try {
        const jsonCurrentCity = localStorage.getItem('selectedCity');

        if (jsonCurrentCity !== undefined) {
            const currentCity = JSON.parse(jsonCurrentCity)
            if (currentCity) {
                newLocation = currentCity
            }
        }
    } catch (e) {
        // localStorage.removeItem('currentUser');
    }
    return newLocation;
  }

  let getSelectedSpace = function(){
    //if nothing selected San Francisco will be default one
    let newSpace;
    // else get selected city info
    try {
        const jsonCurrentSpace = localStorage.getItem('selectedSpace');

        if (jsonCurrentSpace !== undefined) {
            const currentSpace = JSON.parse(jsonCurrentSpace)
            if (currentSpace) {
                newSpace = currentSpace.name
            }
        } else {
          newSpace = 'Women Only';
        }
    } catch (e) {
        // localStorage.removeItem('currentUser');
    }
    return newSpace;

  }

  let isWomenOwnedCircle = function(filteredGroups, title){
    let checkWomenOWned = [];
    if(!_.isEmpty(filteredGroups)){
      for (var circle of filteredGroups) {
        for (var cat of circle.categories) {
          if(cat === 'Woman Owned' && title !== 'Woman Owned'){
            checkWomenOWned.push(true);
          }
        }
      }
    }

    return checkWomenOWned.length !== filteredGroups.length;
  }

  return {
    filterGroups,
    isGroupMuted,
    getAllChatMedia,
    getNearBySavedGroups,
    getSelectedCity,
    getUserCreatedCircles,
    getUserOnlyJoinedCircles,
    getSelectedSpace,
    getGroupUrl,
    isWomenOwnedCircle
  }

}());

export default groupUtiles;
