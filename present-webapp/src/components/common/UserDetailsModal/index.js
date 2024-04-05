import React, {Component} from 'react';
import _ from 'lodash';
import Modal from '../../CircleHome/components/Modal';
import userUtiles from '../../../core/userUtiles';

class UserDetailsModal extends Component {

  constructor(props) {
    super(props);
    this.state = {
      showMemberModal: props.showMemberModal
    }
  }

  render() {
    let user =  this.props.user;
    return(
      <Modal show={this.state.showMemberModal}
        closeModal={() => this.props.onCloseModal()}
        width={true}
        widthSize="341px">
        <img src={require('../../../assets/images/close-btn-black@2x.png')}
             className="member-close-btn"
             onClick={() => this.props.onCloseModal()}
             alt="close"/>
        <div className="member-details text-center">
          <img alt={this.props.user.name} src={user.photo} className="hover-member-img"/>
          <p className="hover-member-name">{user.name}</p>
          {userUtiles.isUserAdmin(this.props.currentUser) ?
          <p>{user.signupLocation}</p> :<span/>}
          <p className="hover-member-bio text-center">{user.bio}</p>
        </div>
      </Modal>
    )
  }
}

export default UserDetailsModal;
