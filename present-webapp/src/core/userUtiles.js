let userUtiles = (function () {

  let userJoinedGroup = function(id, groups){

    let checkTrue = [],
        savedGroups = groups.groups,
        check;

    for (let group in savedGroups) {
        if(savedGroups[group]['uuid'] === id){
          checkTrue.push(true);
        } else {
          checkTrue.push(false);
        }
    }

    if(checkTrue.indexOf(true) > -1){
      check=true;
    } else {
      check=false;
    }
    return check;
  }

  let isUserCircleOwner = function(user, circle){
    if(user.id === circle.owner.id){
      return true;
    } else {
      return false;
    }
  }

  let isUserAdmin = function(user){
    if(user.isAdmin){
      return true;
    } else {
      return false;
    }
  }

  let isUserChatAuthor = function(user,chat){
    let result;
    user.id !== chat.author.id ? result=false : result=true;
    return result;
  }

  let getUserLink = function(user){
    let link = `/u/${user.link.split('/u/')[1]}`;
    return link;
  }

  return {
    userJoinedGroup,
    isUserCircleOwner,
    isUserAdmin,
    isUserChatAuthor,
    getUserLink
  }

}());

export default userUtiles;
