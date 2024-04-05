Pod::Spec.new do |s|
	s.name = 'PresentProtos'
	s.version = '1.0'
	s.summary = 'Generated protos for Present.'
	s.platform = :ios, '10.0'
	s.license = 'Proprietary'
	s.homepage = 'present.co'

	s.author = 'Present'

	s.source = { :git => 'https://github.com/presentco/present.git', :tag => s.version }
	s.source_files = '**/*.{swift}'

	s.dependency 'ProtocolBuffers-Swift', '~> 3.0' # https://github.com/alexeyxo/protobuf-swift
end
