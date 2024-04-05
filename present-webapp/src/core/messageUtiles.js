let messageUtiles = (function () {

  let findOtherParticipant = function (participants, currentUserId) {
    let otherPal={};
    participants.reduce(pal => {
      if(pal.id !== currentUserId){
         otherPal=pal;
      }
    });
    return otherPal;
  };

  let countUnreadMsgs = function(messages){
    let counter=0;

    messages.map(msg => {
      if(!msg.isRead){
        counter++;
      }
    });
    return counter;
  }

  let commentExists = function(newComment, comments){
    let check;
    comments.map(comment => {
      if(comment.uuid === newComment.uuid){
        check = true;
      } else {
        check=false;
      }
    });
    return check;
  };

  let findIndex = function(comments, commentId){
    let index;
    comments.map(comment => {
      if(comment.uuid === commentId){
        index = comments.indexOf(comment);
      }
    });

    return index;
  }

  return {
    findOtherParticipant,
    countUnreadMsgs,
    commentExists,
    findIndex
  }

}());

export default messageUtiles;
