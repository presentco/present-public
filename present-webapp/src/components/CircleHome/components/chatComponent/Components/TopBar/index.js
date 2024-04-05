import React, { Component,PropTypes } from 'react';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import * as userActions from '../../../../../../redux/actions/userActions';
import * as groupActions from '../../../../../../redux/actions/groupActions';
import _ from 'lodash';
import Modal from '../../../Modal';
import MapComponent from '../../../GeneralComponent/Components/MapComponent';
import userUtiles from '../../../../../../core/userUtiles';
import BurgerMenu from './BurgerMenu';

class TopBar extends Component {

  constructor(props){
    super(props);

    this.state={
      currentGroup:{},
      savedGroups: {},
      groupId: props.groupId,
      modalMap:false,
      hideInfo:false,
      currentUser:props.currentUser
    }
  }

  componentWillReceiveProps(nextProps){
    if (this.props.currentGroup !== nextProps.currentGroup) {
      this.setState({currentGroup: nextProps.currentGroup });
    }

    if (this.props.currentUser !== nextProps.currentUser) {
        this.setState({currentUser: nextProps.currentUser });
    }
  }

  render() {
    let modalMap = <div/>,
        groupName = "",
        groupLocation = "";


        if(!_.isEmpty(this.props.currentGroup)){
          groupName= this.props.currentGroup.title;
          groupLocation = this.props.currentGroup.locationName;
          modalMap = (
            <Modal show={this.state.modalMap} closeModal={() => this.setState({modalMap: false})}>
              <div className="relative map-container" >
                <button className="x-btn map-btn" onClick={() => this.setState({modalMap:false})}>X</button>
                <MapComponent isModal={true} />
              </div>
            </Modal>
            )
        }

    return (
      <div >
          <div className="border-bottom-1">
            <div className="row cir-nav">
              <div className="left">
                <button className="back-btn-i" onClick={()=> this.context.router.push('/app')}>
                  <img alt="discovery" src={require('../../../../../../assets/images/back-btn-black.svg')} className="back-btn"/>
                </button>
              </div>
              <div className="middle-container">
                <p className="group-name text-center">{groupName}</p>
                  {groupLocation ? <div className="group-location" >
                      <img alt="location" className="city-icon loc-i" src={require('../../../../../../assets/images/page-1.svg')} />
                      <span className="cursor-poiner" onClick={()=>this.setState({modalMap:true})}>{groupLocation}</span>
                    </div> : <span/>}
              </div>
              <div>
                  {modalMap}
              </div>

              <div className="right" >
                {(!_.isEmpty(this.props.currentGroup) && !_.isEmpty(this.props.currentUser)) && userUtiles.isUserAdmin(this.props.currentUser) && this.props.currentGroup.deleted ?
                <span className="deleted-text">Deleted</span> : <span/>}
                <img alt="info" src={require('../../../../../../assets/images/info.png')}
                  onClick={()=>this.props.hideInfo()}
                  className="info-icon cursor-poiner show-for-small-only" />
                {(!_.isEmpty(this.props.currentGroup) && !_.isEmpty(this.props.currentUser))?
                <BurgerMenu
                  canUserEditCircle={userUtiles.isUserCircleOwner(this.props.currentUser, this.props.currentGroup) || userUtiles.isUserAdmin(this.props.currentUser)}
                  isUserCircleOwner={userUtiles.isUserCircleOwner(this.props.currentUser, this.props.currentGroup)}
                  yourCircle={this.state.currentGroup}
                  />
                : <span/>}
              </div>
            </div>
          </div>
          </div>
        );
    }
}


TopBar.contextTypes = {
 router: PropTypes.object
}

function mapStateToProps(state, ownProps) {
    return {
      groupId: state.group.groupId,
      currentUser: state.user.currentUser,
      currentGroup: state.group.currentGroup,
    }
}

function mapDispatchToProps(dispatch) {
    return {
      actions: bindActionCreators({...userActions,...groupActions}, dispatch)
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(TopBar);
