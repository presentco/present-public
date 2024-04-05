import React, {Component} from 'react';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import './style.css';
import _ from 'lodash';
import ApiHandler from '../../../core/ApiHandler';
import * as userActions from '../../../redux/actions/userActions';
import * as groupActions from '../../../redux/actions/groupActions';


class InvirePresentMembers extends Component {

  constructor(props) {
    super(props);
    this.state = {
      presentMembers:[],
      selectedMember:{}
    }
    this.onChangeOwnerName = this.onChangeOwnerName.bind(this);
    this.onClickSave = this.onClickSave.bind(this);
  }

  onChangeOwnerName(e){
    e.preventDefault();
    let presentMembersNotFriends = [];

    let request = ApiHandler.attachHeader({searchText: _.startCase(_.toLower(e.target.value))})
    this.props.actions.userSearchRequest(request).then(presentMembers => {
      this.setState({presentMembers});
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
        console.log(errorMessage);
    });
  }

  onClickSave(){
    let request = ApiHandler.attachHeader({
      groupId: this.props.yourCircle.uuid,
      ownerId: this.state.selectedMember.id
    });

    this.props.actions.reassignGroup(request).then(response => {
      this.props.closeModal();
      this.props.loadRequestedGroupFromBackend();
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }



  checkImage(friend){
    let src = "select-radio";
    if(friend.id === this.state.selectedMember.id){
        src = "select-radio selected-radio";
    }

    return src;
  }

  checkText(friend){
    let src = "friend-name";
    if(friend.id === this.state.selectedMember.id){
        src = "friend-name selected-text";
    }

    return src;
  }

  onClickMember(member){
    this.setState({selectedMember:member});
  }

   renderList(){

     let showFriendsList = this.state.presentMembers.map((member,index) => {
       return(
         <div className="flex-row cursor-poiner"
              key={index}
              onClick={this.onClickMember.bind(this, member)}>
               <img alt={member.firstName}
                  src={member.photo}
                  className="friend-img"/>
               <p className={this.checkText(member)}>{member.name}</p>
               <div className={this.checkImage(member)}></div>
         </div>
       )
     });
     return showFriendsList;
   }

  render() {

    return(
      <div className="invite-container">
       <div className="flex-row">
         <img onClick={() => this.props.closeModal()} className="change-imge cursor-poiner" src={require('../../../assets/images/close-btn-black@2x.png')} alt="close"/>
         <p className="change-text">Change Circle Owner</p>
         <button
          disabled={!_.isEmpty(this.state.selectedMember) ? false : true}
          className="change-btn"
          onClick={this.onClickSave}>
            Save
         </button>
       </div>
       <div className="friend-search-container">
         <p className="friends-text">Present Members</p>
         <p className="friends-label">Select the owener of this circle</p>
         <div>
           <img src={require('../../../assets/images/search-grey@2x.png')}
             alt="search"
             className="search-gray"/>
           <input
             value={this.state.ownerName}
             onChange={this.onChangeOwnerName}
             className="search-friend-input"
             placeholder="Find friends"/>
         </div>
         {this.state.presentMembers.length !== 0 ?
           <div>
             {this.renderList()}
           </div> : <span/>
         }
       </div>
      </div>
    )
  }

}


function mapStateToProps(state, ownProps) {
    return {
    }
}

function mapDispatchToProps(dispatch) {

    return {
      actions: bindActionCreators({...userActions,...groupActions}, dispatch)
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(InvirePresentMembers);
