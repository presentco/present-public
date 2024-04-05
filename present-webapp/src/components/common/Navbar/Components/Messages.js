import React, { Component } from 'react';
import _ from 'lodash';
import moment from 'moment';
import messageUtiles from '../../../../core/messageUtiles';

class Messages extends Component {
  constructor(props){
    super(props);
    this.state={
      inbox:props.inbox,
      showMore:false
    }
    this.setWrapperRef = this.setWrapperRef.bind(this);
    this.handleClickOutside = this.handleClickOutside.bind(this);
  }

  componentDidMount() {
     document.addEventListener('mousedown', this.handleClickOutside);
 }

 componentWillUnmount() {
     document.removeEventListener('mousedown', this.handleClickOutside);
 }

 setWrapperRef(node) {
     this.wrapperRef = node;
 }

 /**
  * Alert if clicked on outside of element
  */
 handleClickOutside(event) {
     if (this.wrapperRef && !this.wrapperRef.contains(event.target)) {
         this.props.closeModal();
     }
 }

  formatInboxDate(startTime){
    if(moment.unix(startTime/1000).fromNow().indexOf("day") > -1 || moment.unix(startTime/1000).fromNow().indexOf("days") > -1){
      return moment.unix(startTime/1000).format("dddd");
    } else if(moment.unix(startTime/1000).fromNow().indexOf("hours") > -1 || moment.unix(startTime/1000).fromNow().indexOf("hour") > -1 || moment.unix(startTime/1000).fromNow().indexOf("minutes") > -1 || moment.unix(startTime/1000).fromNow().indexOf("minute") > -1 || moment.unix(startTime/1000).fromNow().indexOf("just") > -1 ){
        return moment.unix(startTime/1000).format("h:mm A");
    } else {
        return moment.unix(startTime/1000).format("MMM D");
    }
  }

  isMessageUnread(msg){
    if(msg.messages[0].author.id !== this.props.currentUser.id && !msg.isRead && messageUtiles.countUnreadMsgs(msg.messages) !==0){
      return true;
    } else {
      return false;
    }
  }

  render() {
    let Inbox=<div/>;

    if(this.state.inbox.length !== 0 ){
      let newIndex = this.state.showMore ? this.state.inbox.length : 5
      Inbox = this.state.inbox.map((msg,index) => {

        while(index <= newIndex){
          return (
              <div key={index}
                   className="noti-container">
                   <img
                     src={messageUtiles.findOtherParticipant(msg.participants, this.props.currentUser.id).photo}
                     className={this.isMessageUnread(msg) ? "profile-img change unread" : "profile-img change"}
                     alt="participant"/>
                     {
                       messageUtiles.countUnreadMsgs(msg.messages) !== 0 ?
                       <p className="unread-counter"> {messageUtiles.countUnreadMsgs(msg.messages)}</p>
                         :
                     <span/>
                   }
                   <div className="inbox-container">
                     <p className="inbox-participant">{messageUtiles.findOtherParticipant(msg.participants, this.props.currentUser.id).name}</p>
                     {msg.messages[0].author.id === this.props.currentUser.id ?
                       <p className="you-part">You: </p> :
                         <span/>
                     }
                     <p className={this.isMessageUnread(msg) ? "inbox-text active" : "inbox-text"}>{msg.messages[0].text}</p>
                   </div>
                   <p className="noti-date right">{this.formatInboxDate(msg.messages[0].sentTime)}</p>
              </div>
          )
        }
      })
    } else {
      Inbox=(
        <div className="text-center">
          <p>No messages</p>
        </div>
      );
    }


    return (
      <div className="noti-main-container" ref={this.setWrapperRef}>
        <p className="notf-title more-right">Inbox</p>
        <div className={this.state.inbox.length < 6 ? "" : "main-inbox-container"}>
          {Inbox}
        </div>
        {this.state.inbox.length === 0 || this.state.inbox.length <= 5 ?
          <div/> :
          <div className="text-center">
            <p onClick={() => this.setState({showMore:!this.state.showMore})}
              className="show-more-btn">
              {this.state.showMore ? "Show less" : "Show more"}
            </p>
          </div>
        }
      </div>
    );
  }
}


export default Messages;
