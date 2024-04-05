import React, { Component,PropTypes } from 'react';
import {bindActionCreators} from 'redux';
import {connect} from 'react-redux';
import * as groupActions from '../../../../redux/actions/groupActions';
import ApiHandler from '../../../../core/ApiHandler';
import _ from 'lodash';
import moment from 'moment';


class Notifications extends Component {
  constructor(props){
    super(props);
    this.state={
      notifications:props.notifications,
      showMore:false,
      showGroupJoiner:false
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

 getGroupUrl(group) {

   let test = group.url.split('g/');
   return test[1];
 }

 goToCircle(circle){
   this.context.router.replace(`/g/${circle}`);
   this.props.closeModal();
 }

 goToGroup(noti){
   //USER_COMMENTED_ON_GROUP-->comment
   //"USER_JOINED_GROUP"-->group
   //USER_INVITED_TO_GROUP -->group

   let groupUrl,
      groupId;

   //f(noti.type === "USER_COMMENTED_ON_GROUP" || noti.type === "USER_INVITED_TO_GROUP"){
     groupId= noti.type === "USER_COMMENTED_ON_GROUP" ? noti.defaultTarget.comment.groupId : noti.defaultTarget.group.uuid;
     let request = ApiHandler.attachHeader({groupId:groupId});
     this.props.actions.loadRequestedGroup(request).then(response => {
       groupUrl = this.getGroupUrl(response);
       this.goToCircle(groupUrl);
     }).catch(error => {
       const errorMessage = ApiHandler.getErrorMessage(error);
       console.log(errorMessage);
     });
   //}

 }

   /**
    * Alert if clicked on outside of element
    */
   handleClickOutside(event) {
       if (this.wrapperRef && !this.wrapperRef.contains(event.target)) {
           this.props.closeModal();
       }
   }

  formatDate(when){
    if(moment.unix(when/1000).fromNow().indexOf("day") > -1){
      return moment.unix(when/1000).format("dddd");
    }else if(moment.unix(when/1000).fromNow().indexOf("days") > -1 && moment.unix(when/1000).fromNow().split("days")[0] < 7){
      return moment.unix(when/1000).format("dddd");
    } else if(moment.unix(when/1000).fromNow().indexOf("hours") > -1 || moment.unix(when/1000).fromNow().indexOf("hour") > -1|| moment.unix(when/1000).fromNow().indexOf("minutes") > -1 || moment.unix(when/1000).fromNow().indexOf("minute") > -1 || moment.unix(when/1000).fromNow().indexOf("just") > -1 ){
        return moment.unix(when/1000).fromNow();
    } else {
        return moment.unix(when/1000).format("MMM D");
    }
  }

  render() {
    let Notifications = <div/>;

        if(this.state.notifications.length !== 0){
          let newIndex = this.state.showMore ? this.state.notifications.length : 5
          Notifications = this.state.notifications.map((notification,index) => {
            while(index <= newIndex){
              return (
                  <div key={index}
                       className="noti-container" onClick={this.goToGroup.bind(this,notification)}>
                    <img src={notification.icon} alt="" className="profile-img" />
                    <p className="summary">{notification.summary}</p>
                    <p className="noti-date right">{this.formatDate(notification.when)}</p>
                  </div>
              )
            }
          })
        } else {
          Notifications=(
            <div className="text-center">
              <p>No notifications</p>
            </div>
          );
        }

    return (
      <div className="noti-main-container" ref={this.setWrapperRef}>
        <p className="notf-title">Notifications</p>
        <div className={this.state.notifications.length < 6 ? "" : "main-inbox-container"}>
          {Notifications}
        </div>
        {this.state.notifications.length === 0 || this.state.notifications.length <= 5 ?
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

Notifications.contextTypes = {
 router: PropTypes.object
}

function mapStateToProps(state, ownProps) {
    return {

    }
}

function mapDispatchToProps(dispatch) {
    return {
        actions: bindActionCreators(groupActions, dispatch)
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(Notifications);
