import React, { Component, PropTypes} from 'react';
import _ from 'lodash';

class UserCommunity extends Component {

  constructor(props){
    super(props);

    this.state={
      user: props.user
    }
  }

  getRequestedMember(user){
    this.props.loadRequestedUserFromUrl(user);
  }


  getUserCommunity(){

    let community = <span/>;
    if(this.props.myFriends.length > 0){
        community = this.props.myFriends.map((friend, index) => {
             return (
               <div className="member-main-container" key={index} onClick={this.getRequestedMember.bind(this,friend)}>
                 <img src={friend.photo} alt={friend.firstName} className="community-photos" />
                 <p className="member-name">{friend.firstName}</p>
              </div>)
           })
         }
   return community;
  }

  render(){
    let user = this.props.user,
        firstName = user.firstName ? user.firstName : "";

    return(
      <div className="user-photo-contrainer user-bio-container UserCommunity-container">
        <div className="flex-between-community cursor-poiner">
          <p className="user-header">{!_.isEmpty(this.props.currentUser) && this.props.currentUser.id !== user.id ? `${user.firstName}'s Friends` : "My Friends"}</p>
          {this.props.myFriends.length > 0 ? <p className="right-arrow-view" onClick={() => this.props.openCommunityModal()}>View All</p> : <span/>}
        </div>
        {this.props.myFriends.length === 0 ?
          this.props.type === 'my-friends' ?
          <span className="bio-user-text">You don't have any friends yet</span> :
          <span className="bio-user-text">Add {firstName} as a friend to see their friend’s list</span> :

            <div className="member-container">
              {!_.isEmpty(this.props.currentUser) && this.props.currentUser.id === user.id && this.props.customIncomingFriendsRequests > 0?
                <div className="member-main-container" onClick={() => this.props.openFriendRequestsModal()}>
                  <div className="member-request-number">
                    <span>{this.props.customIncomingFriendsRequests}</span>
                  </div>

                  <img src={require('../../../assets/images/member-request-icon@2x.png')}
                       alt="requests"
                       className="member-img"
                       />
                     <p className="member-name">Requests</p>
                </div>
              : <span/>}

              {this.props.myFriends.length > 0 ? this.getUserCommunity() : <span/>}
            </div>
        }

      </div>
    )
  }
}

UserCommunity.contextTypes = {
  router: PropTypes.object
};


export default UserCommunity;
