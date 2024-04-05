import React, { Component, PropTypes } from 'react';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import * as userActions from '../../../../../../redux/actions/userActions';
import './style.css';
import _ from 'lodash';
import moment from 'moment';
import classnames from 'classnames';
import userUtiles from '../../../../../../core/userUtiles';
import CommentAsText from './CommentAsText';

class Comments extends Component {

  constructor(props){

    super(props);
    this.state={
      ownerIsDeleter:false,
      copied:false,
      chatImageVisible:false,
      media:[],
      currentUser:props.currentUser
    }
    this.copyToClipboard = this.copyToClipboard.bind(this);
    this.createDate= this.createDate.bind(this);
  }

  componentWillReceiveProps(nextProps){
    //Check if the Props for group comments have in fact changed.
    if (this.props.currentUser !== nextProps.currentUser) {
        this.setState({currentUser: nextProps.currentUser });
    }
  }

  createDate(chat){

    let day="";
    //check it the date is for today or yesterday or other day
    if(moment(new Date()).format("DD M") === moment.unix(chat.creationTime/1000).format("DD M") ){
      day = "Today"
    } else if( moment(new Date()).add(-1, 'days').format("DD M") === moment.unix(chat.creationTime/1000).format("DD M") ){
      day = "Yesterday"
    } else{
      day = moment.unix(chat.creationTime/1000).format("MMM  Do ");
    }
    return day;
  }

  onClickDeleteBtn(chat){
    this.props.deleteComment(chat);
  }

  copyToClipboard(text){
    this.setState({copied: !this.state.copied});
  }

  isOwnerOfComment(chat){
    let isSame;
    this.props.currentUser.id === chat.author.id ? isSame = true : isSame = false;
    return isSame;
  }

  isOwnerOfCircle(chat){
    let isOwner;
    this.props.currentUser.id === this.props.currentGroup.owner.id ? isOwner = true : isOwner = false;
    return isOwner;
  }

  renderUserDetails(userPhoto,index,userName,userBio,chat){
    let customClass = classnames(this.props.className, {
       'user-image-container cursor-pointer text-center': chat.content ? true : false,
       'user cursor-pointer text-center' : !chat.content ? true : false
        }),
        that = this;

    return(
      <div className={customClass}>
       <img  src={userPhoto} alt="your" className="user-photo"/>
         <div className={index === 0 || index === 1 || index ===2 ? "show-down-chat text-center" : "show-chat-author text-center"}>
           <img alt={chat.author.name} src={userPhoto} className="hover-member-img"/>
           <p className="hover-member-name">{userName}</p>
           <p className="hover-member-bio">{userBio}</p>

         <button className="send-message-btn" onClick={() => that.context.router.push(`/u/${chat.author.link.split('/u/')[1]}`)}>View Profile</button>
         </div>
       </div>

    )
  }


  render() {
    let {chat,nextComment, index, media} = this.props,
        showDate=<div/>,
        userName= !userUtiles.isUserChatAuthor(this.state.currentUser,chat) ? chat.author.firstName : this.state.currentUser.name.first,
        userPhoto=!userUtiles.isUserChatAuthor(this.state.currentUser,chat) ? chat.author.photo ? chat.author.photo : require('../../../../../../assets/images/profile-photo-placeholder.png') : this.state.currentUser.photo ? this.state.currentUser.photo : require('../../../../../../assets/images/profile-photo-placeholder.png'),
        userBio=!userUtiles.isUserChatAuthor(this.state.currentUser,chat) ? chat.author.bio : this.state.currentUser.bio;

       //if dates are different from the past comments' date
       if(moment.unix(chat.creationTime/1000).format("DD M") !==
       moment.unix(nextComment.creationTime/1000).format("DD M")){

        showDate = (
          <div>
            <p className="show-date-chat">
              {this.createDate(chat)}
            </p>
          </div>
        )
      };
      return (
          <div className="row chat" id={chat.uuid} >
            {showDate}
            <div className="msg-container" >
              {this.renderUserDetails(userPhoto,index,userName,userBio,chat)}

               <CommentAsText
                 media={this.props.media}
                 isAdmin={userUtiles.isUserAdmin(this.props.currentUser)}
                 ref={chat.uuid}
                 resendChat={(chat) => this.props.resendChat(chat)}
                 userName={userName}
                 chat={chat}
                 deleteComment={(chat) => this.onClickDeleteBtn(chat)}
                 isOwnerOfComment={this.isOwnerOfComment(chat)}
                 copyToClipboard={() => this.copyToClipboard()}
                 />

            </div>
          </div>
        )
    }
}

Comments.contextTypes = {
  router: PropTypes.object
}

function mapStateToProps(state, ownProps) {
    return {
      currentUser: state.user.currentUser,
    }
}

function mapDispatchToProps(dispatch) {
    return {
      actions: bindActionCreators(userActions, dispatch)
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(Comments);
