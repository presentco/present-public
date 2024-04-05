
import Foundation
import RxSwift

// A reusable empty view to be shown when a list is empty.
// It shows either a list empty message, or a "no search results found" message.
// It has an optional action button.

public class EmptyView: UIView
{
    private var disposal = DisposeBag()
    var showButton = false {
        didSet {
            button.isShown = showButton
        }
    }
    var emptyText = "No results" {
        didSet {
            mainText.text = emptyText
        }
    }
    
    @IBOutlet weak var button: UIButton! {
        didSet {
            button.roundCornersToHeight()
        }
    }
    
    @IBOutlet weak var mainText: UILabel!
    
    convenience init() {
        self.init(frame: CGRect.zero)
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        initView()
    }
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    private func initView() {
        let view = viewFromNibForClass()
        view.frame = bounds
        addSubview(view)
        button.isShown = showButton
    }
    
    public func bind(searchTextSource: Observable<SearchText>?) {
        searchTextSource?.bind { [weak self] searchText in
            if case .value(let text) = searchText {
                let attributed = NSMutableAttributedString(string: "No results for " + text)
                let boldRange = NSRange(location: attributed.length - text.count, length: text.count)
                let font = UIFont.boldSystemFont(ofSize: 17)
                attributed.addAttribute(NSAttributedStringKey.font, value: font, range: boldRange)
                self?.mainText.attributedText = attributed
                self?.button.isHidden = true
            } else {
                self?.button.isShown = self?.showButton ?? false
                self?.mainText.text = self?.emptyText
            }
        }.disposed(by: disposal)
    }
    
}
